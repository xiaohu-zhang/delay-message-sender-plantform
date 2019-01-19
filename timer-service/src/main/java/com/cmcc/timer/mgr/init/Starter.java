package com.cmcc.timer.mgr.init;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.controller.ipresent.FreezeController;
import com.cmcc.timer.mgr.init.PreLoadModel;
import com.cmcc.timer.mgr.init.messageLoader.LoadStrategy;
import com.cmcc.timer.mgr.netty.SocketRelate;
import com.cmcc.timer.mgr.netty.serverHandle.ServerChannelHandel;
import com.cmcc.timer.mgr.service.store.backlog.BackLogStoreClient;
import com.cmcc.timer.mgr.service.zk.ZkClientExt;
import com.cmcc.timer.mgr.service.zk.ZkRegistryService;
import com.cmcc.timer.mgr.util.ThreadPool;
import com.cmcc.timer.mgr.util.TimeUtil;
import com.cmcc.timer.mgr.util.TimerUtils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;

@Service("starter")
public class Starter implements ApplicationListener<ApplicationReadyEvent>{
    
    @Autowired
    private LoadStrategy loadStrategy;
    
    @Autowired
    private ZkRegistryService zkRegistryService;
    
    //最多扫描多久前的文件，例如，最大的过期时间为2h，则只用扫描2h前的数据文件即可
    @Value("${reloadBeforeSeconds}")
    private long reloadBeforeSeconds;
    
    @Value("${data.path}")
    private String logPath;
    
    @Value("${slave.server.port}")
    private int slaveServerPort;
    
    @Value("${redoFileSize}")
    int fileSizeMax;
    
    @Value("${registry.root.path}")
    private String registryPath;
    
    @Autowired
    private ZkClientExt zkclient;
    
    private ServerBootstrap strap;
    
    @Autowired
    private BackLogStoreClient client;
    
    @Autowired
    private FreezeController freezeController;
  
    public void initLoadByMaster() throws ParseException{
        long sacnTime = -1;
        if(Paths.get(logPath ,"scanlog").toFile().exists()){
            try(FileChannel channel = FileChannel.open(Paths.get(logPath ,"scanlog"), StandardOpenOption.READ)){
                if(channel.size() == 8){
                    ByteBuffer b = ByteBuffer.allocate(8);
                    channel.read(b);
                    b.flip();
                    sacnTime = b.getLong();
                    b.clear();
                }else{
                    logger.error("the scanlog is not only contain the long,check the scanlog content");
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException("maybe in" + logPath + "redolog dictionary or scanlog file on't exit..",e );
            }
        }
        //从zookeeper判断当前机器是否是master，是master则只能按照自身的已有日志全量导入内容,假想最多的msg尚未发送，该接口由zkRegistryservice调用
        
            File redoLogDictionary = Paths.get(logPath ,"redolog").toFile();
            redoLogDictionary.mkdirs();
            long lastModifyTime;
            try {
                //这里不需要向前推时间，因为后面实际执行的时候，会向前推1分钟
                //向前推1分钟是因为主备切换zk能感知到的时间在1min内
                try(Stream<Path> streamPath = Files.list(Paths.get(logPath ,"redolog"))){
                    lastModifyTime = streamPath.mapToLong(p->p.toFile().lastModified()).max()
                            .orElse(-1);
                }
                lastModifyTime = Math.max(lastModifyTime, sacnTime);
                int dateEnd = 0;
                int addHour = 1;
              //50分的数据也需要入到下个时间，此时定时任务无法跑了
                if(LocalDateTime.now().get(ChronoField.MINUTE_OF_HOUR) >= 50){
                    addHour = 2;
                }
                dateEnd = Integer.valueOf(TimeUtil.nextHourlimitToHour(LocalDateTime.now(),addHour));
                //记录下当前load的起止时间
                List<PreLoadModel> needLoadInterval = TimerUtils.getLoadIntervalFromloadlog(logPath);
                
                PreLoadModel merged = new PreLoadModel(lastModifyTime,dateEnd);
                
                needLoadInterval = TimerUtils.mergeIntervel(needLoadInterval, merged);
                
                //对needLoadInterval内的数据进行合并
                
                File loadTimeFile = Paths.get(logPath ,"loadlog").toFile();
                //后续可以考虑采用新写一个temp文件，删除原文件，rename为原文件的方式。
                try(FileWriter fw = new FileWriter(loadTimeFile,false)){
                    needLoadInterval.forEach(p->{
                        try {
                            fw.write(String.valueOf(p.getBeginTimestamp())+"," + String.valueOf(p.getEndTime()) + "\r\n");
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            throw new RuntimeException("maybe in" + loadTimeFile.getAbsolutePath() + "error when write ",e );
                        }
                    });
                }
                
                for(PreLoadModel p : needLoadInterval){
                    load0(p.getBeginTimestamp(),Long.MAX_VALUE,p.getEndTime(),loadTimeFile);
                }
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException("maybe in" + logPath + "redolog dictionary or scanlog file on't exit..",e );
            }
    }
    
    private void load() {
        /**
         * 从文件中读取slot值，连接zookeeper，确定是主备。如果是主。则走以下逻辑，是备，则需要和主进行数据同步。一旦作为slot的主，应该立马启动一个server，用于和client端进行client断线期间的数据。
         * 本机启动以后，作为client连接主。进行以下操作：
         * 1. 启动当前机器的client端，和master主进行通信，比对redolog和主的不同地方，表示是当前机器多的数据，只有add操作多的，需要发送到master端，因为基于时序的问题，如果add-cancel-add，而当前启动的机器发送了多于的
         * cancel操作，时序变为add-add-cancel，这是不允许的，只能发送add操作
         * 2. master将redolog中多余消息，发送给当前机器，同时更新master中记录的master的redolog的读取position。如果下次写入消息到master的时候，发现postion开始值，大于上次记录的postion值，则同步推送可以开启
         * 3. 使用slaveindex记录slave分别的index.slaveIndex文件名每个redolog对应一个index文件，文件格式内容为：
         * 255.255.255.255intintlong,文件名为:yyyyMMddHH.index
         *
         *waring:发送请求的时候，需要加上写入的文件名，保证主从写入的文件是一样的
         */
    }
    
    /**
     * salve 也需要load，要等到和master同步以后，调用load0
     * @param lastModifyTime
     * @param maxModifyTime
     * @param dateEnd
     * @param loadTimeFile
     * @throws IOException
     */
    public void load0(long lastModifyTime,long maxModifyTime,int dateEnd,File loadTimeFile) throws IOException{
        ThreadPool.getThreadPoolExecutor().execute(()->{
            try {
                int beginTime = Integer.valueOf(LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModifyTime),
                        ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMddHH")));
                logger.info("start loading from log ..." + " beginTime: " + beginTime + " endTime:" + dateEnd);
                if(Paths.get(logPath ,"redolog").toFile().exists()){
                    try(Stream<Path> p = TimerUtils.getPathStream(Paths.get(logPath ,"redolog")).get()){
                        p.filter(e->{
                            int deadTime =  Integer.valueOf(e.toFile().getName().substring(5,15));
                            if(deadTime >=  beginTime && deadTime <= dateEnd){
                                return true;
                            }else{
                                return false;
                            }
                        }).sorted((p1,p2)->new Long(p1.toFile().lastModified()).compareTo(
                                p2.toFile().lastModified())).
                        forEachOrdered(p3->{
                            
                            loadStrategy.load(
                                    p3.toAbsolutePath().toString(), true, lastModifyTime ,maxModifyTime);
                        });
                    }
                }
                //删除loadTimeFile文件中的需要load的起止时间
                if(loadTimeFile != null){
                    StringBuilder sb = new StringBuilder();
                    synchronized (TimerUtils.loadTimeFileSynObject) {
                        try(BufferedReader fr = new BufferedReader(new FileReader(loadTimeFile))){
                            String line;
                            while((line = fr.readLine()) != null){
                                if(line.contains(",") && !line.equals(String.valueOf(lastModifyTime) + "," + String.valueOf(dateEnd))){
                                    sb.append(line);
                                }
                            }
                            try(FileWriter fw = new FileWriter(loadTimeFile)){
                                fw.write(sb.toString());
                            }
                        }
                    }
                }
                logger.info("load log successfully..." + " beginTime: " + beginTime + " endTime:" + dateEnd);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                logger.error("open redolog failed " + dateEnd,e);
            }
        });
    }

        @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 初始化ThreadPool，防止后续load数据的时候，回调采用ThreadPool时，尚未进行初始化
        // TODO Auto-generated method stub
        // startServer();
        // master确认时可以做load。client必须等到同步完毕以后才能load
        // initLoadByMaster();
        // 确认TimerUtils的slot,version,如果确实存在，调用注册接口
        if (TimerUtils.slot != -1 && TimerUtils.version != -1) {
            // 确认version是否一致，不一致则不进行自动注册
            if (zkclient.exists(registryPath)) {
                String[] rootData = zkclient.getZkNodeData(registryPath, "\\|\\|");
                if (rootData != null) {
                    for (int i = 0; i < rootData.length; ++i) {
                        String[] slotFromRootData = rootData[i].split("/");
                        if (TimerUtils.slot == Integer.valueOf(slotFromRootData[0])) {
                            if (slotFromRootData[2] != null) {
                                String version = slotFromRootData[2].split("\\^")[0];
                                try {
                                    if (TimerUtils.version == Integer.valueOf(version)) {
                                        int totalSlot = zkclient.getChildren(registryPath).size();
                                        zkRegistryService.register(String.valueOf(TimerUtils.slot), totalSlot);
                                        return;
                                    }
                                } catch (UnknownHostException e) {
                                    // TODO Auto-generated catch block
                                    logger.error("you need to set slave/master ip,call autoRegistry by " + "self", e);
                                } catch (NumberFormatException e) {
                                    logger.error("you need to set slave/master ip,call autoRegistry by " + "self");
                                }
                            }
                        }
                    }
                }
            }
            logger.error("you need to set slave/master ip,call autoRegistry by " + "self");
        }
    }
        
        /**
         * 启动client端,用于和master进行通信
         * 之所以选用client而不是启用server，是因为使用client可以直接和server进行联系，如果启动成server，则需要通知到zookeeper，zookeeper再
         * 通知到master，master启动client端和server连接。这样还消耗了zookeeper的资源，所以由master启动server，slave直接启动client。
         * 主从心跳建议由master发送，这样可以主动感知到slave是否存活，主动断开连接。由slave发送心跳，则master需要启动定时器定时监控是否超时，断开channel连接，有损耗。
         * 但是如果slave比较多，例如app消息推送模式，则必须使用客户端发送心跳的方式，因为主能提供的端口和直接推送心跳的负担都是有限的，需要由客户端发送心跳，server端启动定时任务，
         * 确认关闭连接。
         */
        public InetSocketAddress startServer(){
            strap = new ServerBootstrap();
            strap = strap.group(client.getGroup());
            strap.channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);
            strap.childHandler(new ServerChannelHandel(fileSizeMax,freezeController));
            try {
                ChannelFuture f = strap.bind(0).sync();
                SocketRelate.serverBindChannel = f.channel();
                return (InetSocketAddress)f.channel().localAddress();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e);
            }
            
        }
        
        private int getSlotNodes(){
           return zkclient.countChildren(Paths.get(registryPath, String.valueOf(TimerUtils.slot),"freezeservice").toString());
        }
        
        
        public String getLogPath() {
            return logPath;
        }

        public void setLogPath(String logPath) {
            this.logPath = logPath;
        }



        public ServerBootstrap getStrap() {
            return strap;
        }

        public void setStrap(ServerBootstrap strap) {
            this.strap = strap;
        }

        private Logger logger = LoggerFactory.getLogger(Starter.class);
        
        public static void main(String...strings){
            System.out.println(TimerUtils.getPathStream(Paths.get("c:/cd")).get());
        }
}
