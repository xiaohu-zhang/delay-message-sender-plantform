package com.cmcc.timer.mgr.controller.model;

import java.util.Date;

import com.cmcc.timer.mgr.controller.model.ThreadLocalModel;

public class ThreadLocalModel {
    public  static ThreadLocal<ThreadLocalModel> threadLocal = new ThreadLocal<ThreadLocalModel>();
    
    private String remoteIp;
    private String localIp;
    private String treceId;
    private String url;
    private Date now;
    private String localPort;

    public String getRemoteIp() {
        return remoteIp;
    }
    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }
    public String getLocalIp() {
        return localIp;
    }
    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }
    public String getTreceId() {
        return treceId;
    }
    public void setTreceId(String treceId) {
        this.treceId = treceId;
    }
    
    public ThreadLocalModel(String remoteIp, String localIp, String treceId, String url, Date now,String localPort) {
        super();
        this.remoteIp = remoteIp;
        this.localIp = localIp;
        this.treceId = treceId;
        this.url = url;
        this.now = now;
        this.localPort = localPort;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }
    
    public static ThreadLocalModel getThreadLocal(){
        return threadLocal.get();
    }
    public Date getNow() {
        return now;
    }
    public void setNow(Date now) {
        this.now = now;
    }
	public String getLocalPort() {
		return localPort;
	}
	public void setLocalPort(String localPort) {
		this.localPort = localPort;
	}
    
}
