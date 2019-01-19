package com.cmcc.timer.mgr.controller.model;

import java.util.Date;

/**
 * @ClassName ThreadLocalModel
 * @Description  线程本地变量存储类
 * @author 张安波
 * @date 2017年3月28日 10：53：11
 * @version 2.0.0
 */
public class ThreadLocalModel {
    public final static ThreadLocal<ThreadLocalModel> THREADLOCAL= new InheritableThreadLocal<ThreadLocalModel>();
    
    private String remoteIp;
    private String localIp;
    private String treceId;
    private String url;
    private Date now;
    private String transactionId;
    
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
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    public String getTransactionId() {
        return transactionId;
    }
    public ThreadLocalModel(String remoteIp, String localIp, String treceId, String url, Date now) {
        super();
        this.remoteIp = remoteIp;
        this.localIp = localIp;
        this.treceId = treceId;
        this.url = url;
        this.now = now;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }
    
    public static ThreadLocalModel getThreadLocal(){
        return THREADLOCAL.get();
    }
    public Date getNow() {
        return now;
    }
    public void setNow(Date now) {
        this.now = now;
    }
    
    public static boolean setThreadLocal(ThreadLocalModel value){
        THREADLOCAL.set(value);
        return true;
    }
    
    
}
