package com.cmcc.timer.mgr.service.schedule;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.init.Starter;
import com.cmcc.timer.mgr.service.MasterToSlaveService;
import com.cmcc.timer.mgr.service.store.SyncByteStoreStrategy;
import com.cmcc.timer.mgr.util.FileUtils;
import com.cmcc.timer.mgr.util.ThreadPool;
import com.cmcc.timer.mgr.util.TimeUtil;
import com.cmcc.timer.mgr.util.TimerUtils;

@Service
public class ScheduleService {
    
    @Value("${data.path}")
    private String logPath;
    
    @Autowired
    private Starter loadStart;
    
    @Autowired
    private MasterToSlaveService mTsService;
    
    @Autowired
    private ThreadPool threadPool;
    
    static private FileChannel scanChannel;//scanChannel保持常打开模式
    
    ByteBuffer b = ByteBuffer.allocate(8);
    
    public static  DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd"); 
    
    public static volatile long scanTimestamp;
    
    //每隔一秒向slave发送log
    public void sendToSlave() {
        if(TimerUtils.currentNodetype == 0){
            mTsService.scheduleLogToClient();
        }else{
            synchronized (TimerUtils.slaveWaitObject) {
                try {
					TimerUtils.slaveWaitObject.wait(5 * 60 * 1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					logger.error("error in interrupt ",e);
				}
            }
        }
    }
//    
  //每隔1s执行一次文件大小和文件时间监控,写入时间到scanfile
    public void monitorRedoFile(){
        try {
			if(TimerUtils.currentNodetype == 0){
			    //写入当前时间到scanfile中
			    scanTimestamp = System.currentTimeMillis();
			    b.putLong(scanTimestamp);
			    b.flip();
			    scanChannel.write(b, 0);
			    b.clear();
			}else{
			    synchronized(TimerUtils.slaveWaitObject){
			        TimerUtils.slaveWaitObject.wait(5 * 60 * 1000);
			    }
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("error in put value to scanlog ",e);
		}
    }
//    
//  //每天凌晨三点处理任务
    public void everyDayTask() throws IOException{
        //删除全局map中的过期时间和filename的映射关系
        Map<String,String> map = SyncByteStoreStrategy.getFileByDate();
        String now = LocalDateTime.now().format(TimeUtil.df);
        map.forEach((k,v)->{
            if(k.compareTo(now) < 0){
                map.remove(k);
            }
        });
        
    }
//    
//  //每小时50分将下个小时的数据load到内存
    public void loadNextHour() throws IOException{
        //确认当前时间的redolog已经load过了
    		Long startLoadTimestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).plusHours(1).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli();
    		try {
				loadStart.load0(startLoadTimestamp, Long.MAX_VALUE, Integer.valueOf(TimeUtil.nextHourlimitToHour(LocalDateTime.now(), 1)),null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				logger.error("every 50 minites load error ",e);
			}
    }
    
    
    public static FileChannel getScanChannel() {
        return scanChannel;
    }
    
    @PostConstruct
    public void createScanFile() throws IOException{
        FileUtils.createNewFile(Paths.get(logPath+"/scanlog").toString());
        scanChannel = FileChannel.open(Paths.get(logPath+"/scanlog"), StandardOpenOption.WRITE);
    }
    
    private Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    public static void main(String...strings) throws IOException{
        Path a = Paths.get("d:/","redo.2018050210.log.0");
        Path b = Paths.get("d:/","redo.2018050210.log.1");
        Path c = Paths.get("d:/","redo.2018050211.log.1");
        System.out.println(a.compareTo(b));
        System.out.println(a.compareTo(c));
        System.out.println(b.compareTo(c));
        
    }


}
