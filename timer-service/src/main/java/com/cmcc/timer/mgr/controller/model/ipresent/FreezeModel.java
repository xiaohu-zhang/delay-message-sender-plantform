package com.cmcc.timer.mgr.controller.model.ipresent;

import java.util.Date;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import com.cmcc.timer.mgr.annotation.JsonTimeDeserializer;
public class FreezeModel {
   //用freezesn做equals有问题，发送两笔，则入map一个，入定时队列两个
    
    @NotEmpty
    //解冻的流水号
    private  String freezeSn;
    
    //自动解冻时间 s为单位
    private long delayTime;

   //deat time?
    private Date deadTime;
    
    //创建时间
    //由发送端传入，只有后面的time才能覆盖前面add的model和cancel前面的model
    @NotNull
    private Long createTime;
    
    //取消的消息对应的创建时间
    private long relatedCreateTime;
    
    @NotEmpty
    @Length(min=1,max=256)
    private String gourpId;
    
    @NotEmpty
    @Length(min=1,max=256)
    private String topic;
    
    
    
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
    public long getRelatedCreateTime() {
        return relatedCreateTime;
    }
    public void setRelatedCreateTime(long relatedCreateTime) {
        this.relatedCreateTime = relatedCreateTime;
    }
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
    public long getDelayTime() {
        return delayTime;
    }
    public void setDelayTime(long delayTime) {
        this.delayTime = delayTime;
    }
    
    
    
    
    public Date getDeadTime() {
        return deadTime;
    }
    @JsonTimeDeserializer
    public void setDeadTime(Date deadTime) {
        this.deadTime = deadTime;
    }

    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((freezeSn == null) ? 0 : freezeSn.hashCode());
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
        return result;
    }
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FreezeModel other = (FreezeModel) obj;
		if (freezeSn == null) {
			if (other.freezeSn != null) {
				return false;
			}
		} else if (!freezeSn.equals(other.freezeSn)) {
			return false;
		}
		if (topic == null) {
			if (other.topic != null) {
				return false;
			}
		} else if (!topic.equals(other.topic)) {
			return false;
		}
		return true;
	}
    public String redisKey(){
        return freezeSn+"_"+topic;
    }
    @Override
    public String toString() {
        return "FreezeModel [freezeSn=" + freezeSn + ", delayTime=" + delayTime + ", deadTime=" + deadTime + ", createTime="
                + createTime + ", relatedCreateTime=" + relatedCreateTime + ", gourpId=" + gourpId + ", topic=" + topic + "]";
    }
    
    

    
    
    

    

    

    
    
 
}
