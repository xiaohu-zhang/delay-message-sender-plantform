package com.cmcc.timer.mgr.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public abstract class TimeUtil {
    public static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHH");
    public static DateTimeFormatter dfh = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    
    public static String nextHourlimitToHour(LocalDateTime dt,int hour){
       return  dt.plusHours(hour).format(df);
    }
    
    public static String convertToString(long timestamp,DateTimeFormatter df){
        Date timeDate = new Date(timestamp);
        return timeDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(df);
    }
}
