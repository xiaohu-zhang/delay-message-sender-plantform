package com.cmcc.timer.mgr.service.zk;

import java.net.UnknownHostException;

public interface ZkRegistryClusterService {
    /**
     * 集群注册
     *
     * @param clusterInfo    集群的服务信息
     */
    void registerCluster(String clusterInfo) throws UnknownHostException, InterruptedException;
}
