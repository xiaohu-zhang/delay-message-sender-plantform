package com.cmcc.timer.mgr.service.zk;

import java.net.UnknownHostException;

public interface ZkRegistryService {
    /**
     * 注册服务信息
     *
     * @param slotCode    分组值
     */
    void register(String slotCode,int totalSlot) throws UnknownHostException;
    
    /**
     * 删除当前节点
     */
    public void delNode(String nodeId);
}
