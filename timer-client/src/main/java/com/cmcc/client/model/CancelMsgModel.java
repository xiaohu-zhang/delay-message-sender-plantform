package com.cmcc.client.model;

public class CancelMsgModel{
    private long createTime;
    private String freezeSn;
    private String gourpId;
    private String topic;
    
    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
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
    
    
    
}
