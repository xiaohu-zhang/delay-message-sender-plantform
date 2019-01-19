package com.cmcc.timer.mgr.init;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PreLoadModel {
    long beginTimestamp;
    int endTime;
    int beginTime;
    public PreLoadModel(long beginTimestamp, int endTime) {
        super();
        this.beginTimestamp = beginTimestamp;
        this.endTime = endTime;
        beginTime = Integer.valueOf(LocalDateTime.ofInstant(Instant.ofEpochMilli(beginTimestamp),
                ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMddHH")));
    }
    public long getBeginTimestamp() {
        return beginTimestamp;
    }
    public void setBeginTimestamp(long beginTimestamp) {
        this.beginTimestamp = beginTimestamp;
    }
    public int getEndTime() {
        return endTime;
    }
    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }
    public int getBeginTime() {
        return beginTime;
    }
    public void setBeginTime(int beginTime) {
        this.beginTime = beginTime;
    }
    
}
