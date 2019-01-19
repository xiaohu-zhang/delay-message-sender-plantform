package com.cmcc.timer.mgr.service;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.netty.SocketRelate;
import com.cmcc.timer.mgr.netty.model.SlaveLogPostion;
import com.cmcc.timer.mgr.netty.serverHandle.ClientInboundHandler;
import com.cmcc.timer.mgr.service.schedule.ScheduleService;
import com.cmcc.timer.mgr.service.zk.ZkRegistryService;
import com.cmcc.timer.mgr.util.FileUtils;
import com.cmcc.timer.mgr.util.TimeUtil;
import com.cmcc.timer.mgr.util.TimerUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.DefaultFileRegion;
import io.netty.util.CharsetUtil;
@Service
public class MasterToSlaveService {
    @Autowired
    private ZkRegistryService zkRegistryService;
    
    String logPath = (String) TimerUtils.configMap.get("data.path");
    Path p = Paths.get(logPath, "slaveindex");
    Path redop = Paths.get(logPath,"redolog");
    ByteBuf sendscantimestamp = Unpooled.buffer(13);
    
    {
        sendscantimestamp.writeInt(9);
        sendscantimestamp.writeByte(5);
        sendscantimestamp.writeLong(0);
    }
    
    private int slaveSuffixCompareToRedoSuffix(Path redoFilePath,SlaveLogPostion postion){
        String redoFileName = redoFilePath.toFile().getName();
        int redoFileSuffix = Integer.valueOf(redoFileName.substring(redoFileName.lastIndexOf(".")+1));
        return postion.getSuffix() - redoFileSuffix;
    }
    
    public void scheduleLogToClient() {
        SocketRelate.ctxByAdress.forEach((slaveNodeId, channel) -> {
            // 该连接尚未有返回
            if (!redop.toFile().exists() || !channel.attr(SocketRelate.attributeKey).compareAndSet(true, false)) {
                return;
            }
            try {
                boolean sendScanLogFlag = true;
                // 为防止临界点问题，时间向前移动1h
                LocalDateTime now = LocalDateTime.now();
                now = now.minusHours(1);
                String loadIndexTime = now.format(TimeUtil.df);
                FileUtils.createNewDir(p.toString());
                Supplier<Stream<Path>> supplier = () -> {
                    try {
                        return Files.list(redop).filter(p1 -> {
                            return p1.toFile().getName().substring(5, 15).compareTo(loadIndexTime) >= 0;
                        });
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        logger.error("", e);
                        throw new RuntimeException(e);
                    }
                };
                long timestamp = ScheduleService.scanTimestamp;
                Stream<Path> p3 = supplier.get();
                Stream<Path> p4 = null;
                try {
                    if (p3.count() != 0) {
                        p4 = supplier.get();
                        // 根据redolog进行遍历，如果在slaveIndex中的则直接发送当前文件剩余的数据，如果不在，则直接强制要求slave和master的数据保持一致
                        List<Path> paths = p4.sorted().collect(Collectors.toList());
                        for (Path p1 : paths) {
                            CountDownLatch ch = SocketRelate.channelSyncMap.computeIfAbsent(channel.id(), k -> {
                                return new CountDownLatch(1);
                            });
                            try {
                                SlaveLogPostion postion = ClientInboundHandler.getPosition(slaveNodeId,
                                        p.resolve(p1.toFile().getName().substring(5, 15)));
                                long sendFilelength = p1.toFile().length() - postion.getPosition();
                                if (postion.getSuffix() == -1 || slaveSuffixCompareToRedoSuffix(p1, postion) < 0) {
                                    // slaveIndex不存在，或小于当前redosuffix（说明上次循环时slaveIndex 的suffix已经同步完毕，可以顺序开始同步更大的redo
                                    // suffix文件了）
                                    // 全量发送
                                    sendFilelength =  p1.toFile().length();
                                    sendScanLogFlag = sendFileRegin(channel, p1.toFile(), 0, sendFilelength, ch,
                                            slaveNodeId, true);
                                    postion.setPosition(0);
                                } else if (slaveSuffixCompareToRedoSuffix(p1, postion) == 0) {
                                    // 发送部分数据
                                    if (sendFilelength == 0) {
                                        continue;
                                    } else {
                                        // 发送部分数据
                                        sendScanLogFlag = sendFileRegin(channel, p1.toFile(), postion.getPosition(),
                                                sendFilelength, ch, slaveNodeId, false);
                                    }
                                } else {
                                    // redo suffix小于slaveIndex记录，说明此文件已经同步过了
                                    continue;
                                }
                                // 等待同步数据返回
                                // 正常返回，记录到slaveIndex
                                if (sendScanLogFlag) {
//                                    sendFilelength = p1.toFile().length() - postion.getPosition();
                                    postion.setFileName(p1.toFile().getName());
                                    postion.setSuffix(Integer
                                            .valueOf(p1.toFile().getName().substring(postion.getFileName().lastIndexOf(".")+1)));
                                    saveToSlaveIndexFile(p1.toFile().getName().substring(5, 15), postion, slaveNodeId,
                                            (int) sendFilelength);
                                } else {
                                    // 已经发送失败，则直接跳出循环，不再发送
                                    break;
                                }

                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                throw new RuntimeException("error ", e);
                            } finally {
                                SocketRelate.channelSyncMap.remove(channel.id());
                            }
                        }
                    }
                } finally {
                    p3.close();
                    if (p4 != null) {
                        p4.close();
                    }
                }
                if (sendScanLogFlag) {
                    sendScanLogToSlave(timestamp, channel);
                    channel.attr(SocketRelate.channelnoResponseTimes).set(0);
                }
            } finally {
                channel.attr(SocketRelate.attributeKey).set(true);
            }
        });
    }
                
 
            
    
    private void sendScanLogToSlave(long timestamp,Channel c){
        sendscantimestamp.retain();
        sendscantimestamp.setLong(5, timestamp);
        sendscantimestamp.writerIndex(13);
        sendscantimestamp.readerIndex(0);
        c.writeAndFlush(sendscantimestamp).addListener(future->{
            if (!future.isSuccess()) {
                zkRegistryService.delNode(SocketRelate.getNodeIdByChannel(c).orElse(null));
                SocketRelate.removeChannel(c);
            }
        });
    }
    
    private boolean sendFileRegin(Channel channel,File file,long beginPosition,long length,CountDownLatch ch,String slaveNodeId,boolean overwrite){
        ByteBuf frameLength = Unpooled.copyInt(0);
        frameLength.writeByte(2);
        byte[] fileNmaeBytes = file.getName().getBytes(CharsetUtil.UTF_8);
        if(overwrite){
            frameLength.writeByte(1);
        }else{
            frameLength.writeByte(0);
        }
        frameLength.writeByte(fileNmaeBytes.length);
        frameLength.writeBytes(fileNmaeBytes);
        frameLength.setInt(0, (int)length + frameLength.readableBytes() - 4);
        try {
                frameLength.retain();
                channel.write(frameLength, channel.voidPromise());
                channel.writeAndFlush(new DefaultFileRegion(file, beginPosition, length),channel.voidPromise());
                boolean sendResponse = ch.await(30, TimeUnit.SECONDS);
                if(sendResponse == true){
                    //外部关闭此channel
                    if(!channel.isActive()){
                        return false;
                    }
                    return true;
                }
                
                if(channel.attr(SocketRelate.channelnoResponseTimes).get() >= 1){
                    zkRegistryService.delNode(SocketRelate.getNodeIdByChannel(channel).orElse(null));
                    SocketRelate.removeChannel(channel);
                }else{
                    int value = channel.attr(SocketRelate.channelnoResponseTimes).get() + 1;
                    channel.attr(SocketRelate.channelnoResponseTimes).set(value);
                }
            //三次无法访问
            logger.error("error in get response from peer " + channel.remoteAddress() + " have retried 3 times,close connection");
            return false;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
           logger.error("",e);
        } finally {
            frameLength.release();
        }
        return false;
    }
    //这里后期要缓存下对应的postion，不需要从文件中获取
    public void saveToSlaveIndexFile(String yyyyMMddHH,SlaveLogPostion slavePostion,String slaveNodeId,int sendFileLength) throws IOException{
        Path p = Paths.get((String)TimerUtils.configMap.get("data.path") ,"slaveindex",yyyyMMddHH);
        ByteBuf writeBuf = Unpooled.buffer(44);
        writeBuf.writeBytes(slaveNodeId.getBytes(CharsetUtil.UTF_8));
        writeBuf.writeInt(slavePostion.getSuffix());
        writeBuf.writeLong(slavePostion.getPosition() + sendFileLength);
                
        ByteBuf loadBuf = Unpooled.buffer(44);
        FileUtils.createNewFile(p.toString());
        try(FileChannel fc = FileChannel.open(p, StandardOpenOption.READ,StandardOpenOption.WRITE)){
            loadBuf.writeBytes(fc, 0l, (int)fc.size());
            while(loadBuf.readableBytes() > 0){
                String uuid = (String)loadBuf.readCharSequence(32, CharsetUtil.UTF_8);
                if(uuid.equals(slaveNodeId)){
                    writeBuf.readBytes(fc, loadBuf.readerIndex()-32, 44);
                    return;
                }
                loadBuf.readerIndex(loadBuf.readerIndex() + 12);
            }
            writeBuf.readBytes(fc, fc.size(), 44);
        }
    }
    
    private Logger logger = LoggerFactory.getLogger(MasterToSlaveService.class);
}
