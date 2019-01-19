package com.cmcc.timer.mgr.service.store;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.service.store.backlog.BackLogStoreStrategy;
import com.cmcc.timer.mgr.util.FileUtils;

import io.netty.buffer.ByteBuf;

public abstract class SyncByteStoreStrategy implements StoreStrategy {
    @Value("${data.path}")
    private String logPath;

   
    
    
    @Value("${redoFileSize}")
    long fileSizeMax;
    
    public static  DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMddHH");
    
    private AtomicInteger fileSizeCheck = new AtomicInteger();
    @Autowired
    private BackLogStoreStrategy backLogStoreStrategy;
    
    private static int fileSizeCheckMod = 2 << 10 -1;
    
    private static Map<String,String> fileByDate = new ConcurrentHashMap<>(2);
    
    
    @Override
    /**单条记录格式:
     * message(bytes)
     * deadline(long)
     * createTime(long)
     */
    public void store(FreezeModel model,String prefixpath) {
        LocalDateTime deadTime = LocalDateTime.ofInstant(model.getDeadTime().toInstant(),ZoneId.systemDefault());
        ByteBuf redoBuf = fillWriteBufWithOutLength(model);
        try (FileOutputStream    fosData = new FileOutputStream(getFilePath(deadTime).toString(), true);FileChannel channel =fosData.getChannel()) {
//            synchronized (SyncByteStoreStrategy.class) {
//                channel.position(channel.size());
                ByteBuffer bf = redoBuf.nioBuffer();
                while(bf.hasRemaining()){
                    channel.write(bf);
                }
            }
catch (Exception e) {
           throw new RuntimeException("error in output File redo.log at " + getFilePath(deadTime).toString(),e);
        }
        //TODO 往客户端的queue中添加 redoBuf
//        backLogStoreStrategy.addBackLog(redoBuf);
    }



    protected abstract ByteBuf fillWriteBufWithOutLength(FreezeModel model);
    
    private Path getFilePath(LocalDateTime t) {
        int num = fileSizeCheck.incrementAndGet();
        String fileDate = t.format(dateFormat);
        String fileName = fileByDate.get(fileDate);
        if (fileName != null) {
            if ( (num & fileSizeCheckMod) == 0 && fileSizeMax < new File(fileName).length()) {
                // >500M
                int suffix = Integer.valueOf(fileName.substring(fileName.lastIndexOf(".") + 1));
                suffix++;
                fileName = fileName.substring(0, fileName.lastIndexOf(".") + 1) + suffix;
            }else{
                return Paths.get(fileName);
            }
        } else {
            fileName = logPath + "/redolog/redo." + t.format(dateFormat) + ".log.0";
        }
        FileUtils.createNewFile(fileName);
        fileByDate.put(fileDate, fileName);
        return Paths.get(fileName);
    }



    public static Map<String, String> getFileByDate() {
        return fileByDate;
    }




    

    
}
