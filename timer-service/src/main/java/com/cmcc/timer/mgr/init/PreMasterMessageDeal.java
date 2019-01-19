package com.cmcc.timer.mgr.init;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.controller.model.ipresent.OpEnum;
import com.cmcc.timer.mgr.init.PreLoadModel;
import com.cmcc.timer.mgr.netty.SocketRelate;
import com.cmcc.timer.mgr.service.store.backlog.BackLogStoreClient;
import com.cmcc.timer.mgr.service.zk.ZkClientExt;
import com.cmcc.timer.mgr.service.zk.ZkRegistryService;
import com.cmcc.timer.mgr.util.TimeUtil;
import com.cmcc.timer.mgr.util.TimerUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.util.CharsetUtil;

@Service
public class PreMasterMessageDeal {
    @Value("${data.path}")
    private String logPath;
    
    @Autowired
    private BackLogStoreClient client;
    
    @Autowired
    private ZkClientExt zkClient;
    
    @Value("${registry.root.path}")
    private String registryPath;
    
    @Autowired
    private ZkRegistryService zkRegistryService;
    

    
    /**
     * index 文件格式 定长文件 最大长度为32 + 4 + 8 =44不足在ip最后补空格,int表示redo文件的最后一个后缀最大长度格式为:
     * uuidintlong
     * uuid --- 32B
     * int --- file suffix
     * long --- file position
     * 
     * @throws IOException
     */
    public void dealNotSenderMessage(String masterUuid) {
        Path p = Paths.get(logPath ,"slaveindex");
        if(p.toFile().exists()){
            ByteBuffer ipUuid = ByteBuffer.allocate(32);
            ByteBuffer suffixb = ByteBuffer.allocate(4);
            ByteBuffer filePosition = ByteBuffer.allocate(8);
            String yyyyMMddHHScan = "1970010100";
            long scantimeStamp = 0;
            if(Paths.get(logPath,"scanlog").toFile().exists() && Paths.get(logPath,"scanlog").toFile().length() > 0){
                ByteBuf scanFileBuf = Unpooled.buffer(8);
                try(FileChannel fc = FileChannel.open(Paths.get(logPath,"scanlog"), StandardOpenOption.READ)){
                    scanFileBuf.writeBytes(fc, 0, 8);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(e1);
                }
                scantimeStamp = scanFileBuf.readLong();
                Date scanDate = new Date(scantimeStamp);
                yyyyMMddHHScan = LocalDateTime.ofInstant(scanDate.toInstant(),ZoneId.systemDefault()).format(TimeUtil.df);
            }
            final String scanString = yyyyMMddHHScan;
            //slave遍历redolog文件夹，得到最大的截止时间文件时间t。从scanString到t之间的文件全部同步到当前的主
            //找到最大的redo文件名
            Stream<Path>[] redos = new Stream[3];
            try {
                for(int i = 0;i < redos.length;++i){
                    redos[i] = TimerUtils.getPathStream(Paths.get(logPath,"redolog")).get();
                }
            if(Paths.get(logPath,"redolog").toFile().exists() && redos[0].count() > 0 ){
                String maxRedoName = redos[1].map(p1->p1.toFile().getName().substring(5, 15))
                .max((n1,n2)->{
                   return n1.compareTo(n2);
                }
                ).get();
                List<PreLoadModel> loadIntervals = TimerUtils.getLoadIntervalFromloadlog(logPath);
                PreLoadModel model = new PreLoadModel(scantimeStamp,Integer.valueOf(maxRedoName));
                List<PreLoadModel> loadIntervalsf = TimerUtils.mergeIntervel(loadIntervals, model);
                //这里注意并不需要将model写入到load文件中，因为
                Set<String> sendFils = new HashSet<>();
                redos[2].filter(e->{
                   int deadTime =  Integer.valueOf(e.toFile().getName().substring(5,15));
                   for(PreLoadModel l: loadIntervalsf){
                       if(deadTime >=  l.getBeginTime() && deadTime <= l.getEndTime()){
                           return true;
                       }
                   }
                   return false;
                }).sorted().forEachOrdered(p1->{
                            //1. 判断p1是否在slaveIndex中，是则发送index后面的数据,如果slaveIndex中的suffix>redo中的suffix，则说明这个redo已经同步过，无需同步
                            //2. 如果不在，则全部文件直接发送
                            //3. 同一个时间点考虑文件可以是多个的情况
                            Path slaveIndexPath = Paths.get(p.toString(),p1.getFileName().toString().substring(5, 15));
                            boolean sendWholeFile = true;
                            if(slaveIndexPath.toFile().exists()){
                                try(FileChannel fc = FileChannel.open(slaveIndexPath, StandardOpenOption.READ)){
                                    int readBeginPostion = 0;
                                    for(;readBeginPostion < fc.size();readBeginPostion += 44){
                                    	fc.position(readBeginPostion);
                                        fc.read(ipUuid);
                                        ipUuid.flip();
                                        String mUid = new String(ipUuid.array(),CharsetUtil.UTF_8);
                                        ipUuid.clear();
                                        //这里如果都不存在，可能需要将scanlog记录时间后的全量的数据全部导入到主那里 
                                        if(masterUuid.trim().equals(mUid)){
                                            fc.read(suffixb);
                                            suffixb.flip();
                                            int suffix = suffixb.getInt();
                                            suffixb.clear();
                                            int logSuffix = Integer.valueOf(p1.toFile().getName().substring(p1.toFile().getName().lastIndexOf(".")+1));
                                            if(suffix > logSuffix){
                                                sendWholeFile = false;
                                                break;
                                            }else if(suffix < logSuffix){
                                               break;
                                            }
                                            fc.read(filePosition);
                                            filePosition.flip();
                                            long postion = filePosition.getLong();
                                            filePosition.clear();
                                            //将对应文件内容直接发送到master处
                                            String indexFileName = slaveIndexPath.toFile().getName();
                                            String redoLogName = "redo." + indexFileName.substring(0,10) + ".log." + suffix;
                                            int sendResult = sendAddMsg(mUid, postion, redoLogName);
                                            readBeginPostion = -1;
                                            sendWholeFile = false;
                                            break;
                                        }
                                    }
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    throw new RuntimeException(e);
                                }
                            }
                            if(sendWholeFile){
                                try {
                                    sendAddMsg(masterUuid, 0, p1.toFile().getName());
                                } catch (IOException e1) {
                                    // TODO Auto-generated catch block
                                    logger.error("",e1);
                                }
                            }
                        });
                
                
                //如果loadlog文件存在，直接清空该文件
                try(FileWriter fw = new FileWriter(Paths.get(logPath,"loadlog").toFile())){
                  fw.write("");
              }
            }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                for(Stream<Path> s : redos){
                    s.close();
                }
            }
        }
        //删除slaveIndex文件夹
        TimerUtils.rMDeleteFile(Paths.get(logPath,"slaveindex"));
        //删除全部的redoLog。因为如果不删除，则可能有当前的备redo的suffix大于主的suffix的情况。这种情况下，此备再切换为主的时候，和原主数据存在严重不一致。和其他的从数据也严重不一致。
        TimerUtils.rMDeleteFile(Paths.get(logPath,"redolog"));
        //发送完毕以后，删除slaveIndex，因为如果此时改节点变为master节点，依然需要删除slaveIndex。同时，删除以后还能保证再次启动为slave的时候不会重现向主发送数据
      //全部数据发送完毕，发送一个表示slave发送到master完毕的请求,接下来master可以向slave进行数据同步
        ByteBuf sendOverbuf = Unpooled.buffer(5);
        sendOverbuf.writeInt(1);
        sendOverbuf.writeByte(6);
        SocketRelate.ctxByAdress.get(masterUuid).writeAndFlush(sendOverbuf);
    }

    private int sendAddMsg(String mUid, long postion, String redoLogName) throws IOException {
        try(FileChannel redofc = FileChannel.open(Paths.get(logPath ,"redolog",redoLogName), StandardOpenOption.READ)){
            //得到postion之间的数据
            int sendBufSize = (int)(redofc.size() - postion);
            int remainBufSize = sendBufSize;
            if(remainBufSize == 0){
                return -1;
            }
            FileRegion region = new DefaultFileRegion(redofc, postion, sendBufSize);
            //直接通过FileRegion发送数据
            Channel  ctx = SocketRelate.ctxByAdress.get(mUid);
            ByteBuf sendBuffer = Unpooled.buffer(6);
            try {
            ctx.attr(SocketRelate.slaveToMasterKey).set(new CountDownLatch(1));
            int i;
            for( i = 0;i < 3;++i){
                sendBuffer.retain();
                region.retain();
                sendBuffer.clear();
                sendBuffer.writeInt(sendBufSize + 1 +1);
                sendBuffer.writeByte(5);
                sendBuffer.writeByte(1);
                ctx.write(sendBuffer);
                ctx.writeAndFlush(region,ctx.voidPromise());
                boolean sendResieve = ctx.attr(SocketRelate.slaveToMasterKey).get().await(30, TimeUnit.SECONDS);
                if(sendResieve){
                    break;
                }
            }
            if(i == 3){
                //删除当前临时节点，slave节点注册失败,断开所有channel连接
                //zk 监听child节点，master节点删除和slave之间的联系
                logger.error("slave register failed,because slave(original master) send to master redolog 30s master no response retry 3 times");
                SocketRelate.clear();
                zkRegistryService.delNode(TimerUtils.getNodeId());
                throw new RuntimeException("slave register failed,because slave(original master) send to master redolog 30s master no response retry 3 times");
            }
            }catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                   logger.error("interupted ",e);
                }
            }
            
            
            
            
//            redofc.read(sendBuffer, postion);
//            ByteBuf sendBuf = Unpooled.wrappedBuffer(sendBuffer);
//            // 1 表示发送的是client多于master的数据
//            ByteBuf sendBufStract =  stract(sendBuf);
//            postion = 0;
//            remainBufSize = sendBufStract.readableBytes();
//            while(remainBufSize > 0){
//                ByteBuf buf = Unpooled.buffer();
//                /**
//                 *  发送格式
//                 *  framelength -- int
//                 *  type(5 client多于master的数据) -- byte
//                 *  lastblock -- byte (0 还有后续block 1 已经是最后的block)
//                 *  msg ---client多余的内容
//                 */
//                buf.writeInt(0);
//                buf.writeByte(5);
//                remainBufSize -= sendBufSize;
//                if(remainBufSize == 0){
//                    buf.writeByte(1);
//                }else{
//                    buf.writeByte(0);
//                }
//                //这里不能直接取2M大小，必须抽取出正好整数的msg
//                sendBufStract.readerIndex((int)postion);
//                ByteBuf sendMsg = TimerUtils.stractByte(sendBufStract, 2 * 1024 * 1024);
//                sendBufSize = sendMsg.readableBytes();
//                postion += sendBufSize;
//                //开启tcp，远程传输sendbufStract
//                int length = buf.writerIndex() + sendMsg.writerIndex();
//                buf.markWriterIndex();
//                buf.writerIndex(0);
//                buf.writeInt(length - 4);
//                buf.resetWriterIndex();
//                ctx.write(buf);
//                ctx.writeAndFlush(sendMsg);
        return 1;
            }
    
    /**
     * 向master发送4，报告自身的nodeId
     */
    public void sendNodeId(){
        String nodeId = client.getMasterNodeId();
        ByteBuf sendbuf = Unpooled.buffer();
        sendbuf.writeInt(33);
        sendbuf.writeByte(4);
        sendbuf.writeBytes(TimerUtils.getNodeIdBytes());
        Channel clientServerChannel = SocketRelate.ctxByAdress.get(nodeId);
        clientServerChannel.writeAndFlush(sendbuf,clientServerChannel.voidPromise());
    }
    
    private ByteBuf stract(ByteBuf sendBuf){
        ByteBuf returnBf = Unpooled.buffer(0,sendBuf.capacity());
        while(sendBuf.readerIndex() < sendBuf.capacity()){
            sendBuf.markReaderIndex();
            int lineSize = sendBuf.readInt();
            int opCode = sendBuf.readByte();
            sendBuf.resetReaderIndex();
            if(opCode == OpEnum.Add.getValue()){
                returnBf.writeBytes(sendBuf.slice(sendBuf.readerIndex(), lineSize));
            }
            sendBuf.readerIndex(sendBuf.readerIndex() + lineSize );
        }
        return returnBf;
    }
    
    private static Logger logger = LoggerFactory.getLogger(PreMasterMessageDeal.class);
    public static void main(String...strings) throws IOException{
        Integer[] k = new Integer[3];
        for(Integer r : k){
            r = 4;
        }
        
    }
    
}
