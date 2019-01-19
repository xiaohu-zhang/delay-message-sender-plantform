package com.cmcc.timer.mgr.controller.model.ipresent;

public class ZkRegistryModel {
    private String clusterInfo;

    private String slotCode;
    
    private int totalSlot;
    
    //0 ---master
    //1 ---slave
    private int type;
    
    private int masterTcpPort;

    public String getClusterInfo() {
        return clusterInfo;
    }

    public void setClusterInfo(String clusterInfo) {
        this.clusterInfo = clusterInfo;
    }

    public String getSlotCode() {
        return slotCode;
    }

    public void setSlotCode(String slotCode) {
        this.slotCode = slotCode;
    }

    public int getTotalSlot() {
        return totalSlot;
    }

    public void setTotalSlot(int totalSlot) {
        this.totalSlot = totalSlot;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getMasterTcpPort() {
        return masterTcpPort;
    }

    public void setMasterTcpPort(int masterTcpPort) {
        this.masterTcpPort = masterTcpPort;
    }
    
    
}