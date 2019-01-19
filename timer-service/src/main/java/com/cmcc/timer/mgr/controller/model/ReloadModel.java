package com.cmcc.timer.mgr.controller.model;

import java.util.Date;

import com.cmcc.timer.mgr.annotation.JsonTimeDeserializer;

public class ReloadModel {
    
    private Date beginTime;
    
    private Integer endTime;
    public Date getBeginTime() {
        return beginTime;
    }
    @JsonTimeDeserializer
    public void setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
    }
    public Integer getEndTime() {
        return endTime;
    }
    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }

    
}
