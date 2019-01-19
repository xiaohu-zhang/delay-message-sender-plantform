package com.cmcc.timer.mgr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import com.cmcc.timer.mgr.init.PreLoadModel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

public abstract class TimerUtils {
    //本机的唯一标识符
    public static byte[] uuid;
    
    public static String uuidString;
    
    public static String masterId;
    
    public static int currentNodetype = 1;//0 master 1 slave
    
    public static int slot = -1;
    
    public static int version = -1;
    
    public static boolean initLoadSuccess = false;
    
    public static Map<String,Object> configMap = new HashMap<>();
    
    public static int TOTALSOLTNUMBER = 0x4000;
    
    public static Object slaveWaitObject = new Object();
    
    public static Object loadTimeFileSynObject = new Object();
    
    public static Object slaveIndexDirSyn = new Object();
    
    public static long makeSlaveIndexDirtimestamp ;
    
    public static Path configPath;
    
    public static String getSourceAddrFromCtx(ChannelHandlerContext ctx){
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String ipAndPort = socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
        return ipAndPort;
    }
    
    public static void storeByte(Path filePath,ByteBuf content){
        try(FileChannel fc = FileChannel.open(filePath,StandardOpenOption.WRITE)){
            ByteBuffer c = content.nioBuffer();
            while(c.hasRemaining()){
                fc.write(c, fc.size());
            }
        }catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException("read in" + filePath.toFile().getAbsolutePath() + "error ",e );
        }
    }
    
    public static void rMDeleteFile(Path p){
//        if(PlatformDependent.isWindows()){
//            File temp = new File("t.temp");
//            String tempPath = temp.getAbsolutePath();
//            p = Paths.get(tempPath.substring(0, tempPath.indexOf(":")+1), p.toFile().getAbsolutePath());
//        }
        if(p.toFile().exists()){
            if(p.toFile().isDirectory()){
                Stream<Path> p2  = TimerUtils.getPathStream(p).get();
                try {
                    p2.forEach(p1->{
                        rMDeleteFile(p1);
                    });
                    p.toFile().delete();
                } finally {
                        p2.close();
                }
            }else{
                p.toFile().delete();
            }
        }
    }
    
    /**
     * 注意这里截取的是原bytebuf的一个slice。
     */
    public static ByteBuf stractByte(ByteBuf source , long maxBytes){
        long nextLength = 0;
        int currentPosition = source.readerIndex();
        int originReaderIndex = currentPosition;
        int msgLength = source.readInt();
        for(nextLength += msgLength;nextLength < maxBytes;msgLength = source.readInt(),nextLength += msgLength){
            currentPosition += msgLength;
            source.readerIndex(currentPosition);
        }
        return source.slice(originReaderIndex, currentPosition-1);
    }
    
    public static void propertiesToMap(Properties p){
        p.forEach((k,v)->{
            configMap.put((String)k, v);
        });
    }
    
    /**
     * logPath 格式
     * nodeId 32 -- 本机nodeId
     * slot 4 --- 本机对应的的slot
     * version 4 --- 本机对应的slot version
     * 
     */
    public static void init() {
        File applicationFile = Paths.get(configPath.toString(),"application" + ".yaml").toFile();
        YamlPropertiesFactoryBean yamlMapFactoryBean = new YamlPropertiesFactoryBean();
        yamlMapFactoryBean.setResources(new FileSystemResource(applicationFile)); 
        String activeYamlSuffix = (String) yamlMapFactoryBean.getObject().get("spring.profiles.active");
        File configFile = Paths.get(configPath.toString(),"application-" + activeYamlSuffix + ".yaml").toFile();
        yamlMapFactoryBean.setResources(new FileSystemResource(configFile)); 
        propertiesToMap(yamlMapFactoryBean.getObject());
        if(uuidString == null){
            String logPath = (String) configMap.get("data.path");
            File nodeConfigFile = FileUtils.createNewFile(Paths.get(logPath, "node.config").toFile().getAbsolutePath());
            try(FileChannel f = FileChannel.open(nodeConfigFile.toPath(),StandardOpenOption.READ,StandardOpenOption.WRITE)){
                ByteBuffer nodeId = ByteBuffer.allocate(72);
                if(f.read(nodeId) == 40){
                    nodeId.flip();
                    ByteBuf nodeB = Unpooled.wrappedBuffer(nodeId);
                    uuidString = nodeB.toString(0, 32, CharsetUtil.UTF_8);
                    uuid = uuidString.getBytes(CharsetUtil.UTF_8);
                    nodeId.position(32);
                    slot = nodeId.getInt();
                    version = nodeId.getInt();
                    
                }else{
                    uuidString = CUUID.createUuid();
                    uuid = uuidString.getBytes(CharsetUtil.UTF_8);
                }
            }catch (Exception e) {
                // TODO: handle exception
        }
        }
    }
    
    public static List<PreLoadModel> getLoadIntervalFromloadlog(String logPath) throws FileNotFoundException, IOException{
        File loadTimeFile = Paths.get(logPath ,"loadlog").toFile();
        FileUtils.createNewFile(loadTimeFile.getAbsolutePath());
        List<PreLoadModel> needLoadInterval = new LinkedList<>();
        String lines;
        try(BufferedReader fr = new BufferedReader(new FileReader(loadTimeFile))){
            while((lines = fr.readLine()) != null){
                if(lines.contains(",")){
                    String[] times = lines.split(",");
                    long beginTime = Long.valueOf(times[0]);
                    int endTime = Integer.valueOf(times[1]);
                    needLoadInterval.add(new PreLoadModel(beginTime, endTime));
                }
            }
        }
        return needLoadInterval;
    }
    
    public static List<PreLoadModel> mergeIntervel(List<PreLoadModel> src , PreLoadModel merged) throws ParseException{
        List<PreLoadModel> dst = src.stream().collect(Collectors.toList());
        
        //对needLoadInterval内的数据进行合并
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHH");
        
        long mergeEnd = -1;
        for(PreLoadModel p : dst){
            long r0 = p.getBeginTimestamp();
            long e0 = df.parse(String.valueOf(p.getEndTime())).getTime();
            long r1 = merged.getBeginTimestamp();
            long e1 = df.parse(String.valueOf(merged.getEndTime())).getTime();
            if(r1 == Math.min(r0, r1)){
                long temp = 0;
                temp = r0;
                r0 = r1;
                r1 = temp;
                temp = e0;
                e0 = e1;
                e1 = temp;
            }
            if(r1 >= r0 && r1 <= e1){
                mergeEnd = Math.max(e1, e0);
                p.setBeginTimestamp(r0);
                p.setEndTime(Integer.valueOf(df.format(new Date(mergeEnd))));
                break;
            }
        }
        if(mergeEnd == -1){
            dst.add(merged);   
        }
        return dst;
    }
    
    public static String getMasterId() {
        return masterId;
    }

    public static void setMasterId(String masterId) {
        TimerUtils.masterId = masterId;
    }

    public static String getNodeId(){
        return uuidString;
    }
    
   public static byte[] getNodeIdBytes(){
       return uuid;
   }
    
    public static boolean incurrentHour(String inputTime){
        if(LocalDateTime.now().format(TimeUtil.df).equals(inputTime)){
            return true;
        }
        return false;
    }
    
    public static Supplier<Stream<Path>> getPathStream(Path p){
        return ()->{
            try {
                    return Files.list(p);
            } catch (IOException e) {
                // TODO Auto-generated catch block
               throw new RuntimeException(e);
            }
        };
    }
    
    public static void main(String...strings) throws IOException{
        System.out.println(System.currentTimeMillis());
    }
    
    private static Logger logger = LoggerFactory.getLogger(TimerUtils.class);
}
