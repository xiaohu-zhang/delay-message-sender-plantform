package com.cmcc.client.model;

import java.util.Date;

public class SendMsgModel {
    private Date deadTime;
    private String freezeSn;
    private String gourpId;
    private String topic;
    private long createTime;
    public Date getDeadTime() {
        return deadTime;
    }
    public void setDeadTime(Date deadTime) {
        this.deadTime = deadTime;
    }
    public String getFreezeSn() {
        return freezeSn;
    }
    public void setFreezeSn(String freezeSn) {
        this.freezeSn = freezeSn;
    }
    public String getGourpId() {
        return gourpId;
    }
    public void setGourpId(String gourpId) {
        this.gourpId = gourpId;
    }
    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }
	public long getCreateTime() {
		return createTime;
	}
	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}
    
}
