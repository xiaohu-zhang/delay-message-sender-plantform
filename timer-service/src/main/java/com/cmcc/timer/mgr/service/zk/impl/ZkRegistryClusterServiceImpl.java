package com.cmcc.timer.mgr.service.zk.impl;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.cmcc.timer.mgr.ExceptionHandler.MgrExceptionFactory;
import com.cmcc.timer.mgr.service.zk.ZkRegistryClusterService;
import com.cmcc.timer.mgr.service.zk.ZkRegistryService;
import com.cmcc.timer.mgr.util.HeaderUtil;
import com.cmcc.timer.mgr.util.HttpUtil;

/**
 * 服务注册集群
 * 解析集群信息，然后分别调用单点注册方法
 * 1. 信息的格式为：172.28.20.100:8080|172.28.20.101:8080||172.28.20.200:8080|172.28.20.201:8080
 * 其中，||表示分组，数组所在的序号为组号；|表示组内的机器
 * zookeeper节点示意
 * 
 *                                              |---/address-001(temp node)
 *             |----/0(slot)/freezeservice/---- |---/address-002(temp node)
 *  /timer ----
 *             |----/1(slot)/freezeservice/---- |---/address-001(temp node)
 *                                              |---/address-002(temp node)
 *                                              
 *  timer 存储数据           0/0-8292/version^uuid->ip:httpPort:tcpPort||1/8293-16384/version^uuid->ip:httpPort:tcpPort   8293 = 16384/2+1   hash(key)/8293 的商 入对应的slot. ip:port均指master的。port为 随机 bind的server端端口号                         
 *  address-001 存储数据 version^nodeId(uuid)->ip:httpPort:tcpPort(用于主备节点的复制) version是版本号，为了后期做扩/缩容后，版本号要加一,初始值为1
 *  address-002 存储数据 version^nodeId(uuid)->ip:httpPort
 *  /timer/0 存储数据  version^nodeId(uuid)->ip:httpPort:tcpPort|version^nodeId(uuid)->ip:httpPort|version^nodeId(uuid)->ip:httpPort
 *                                              
 *                                              
 */



@Component
public class ZkRegistryClusterServiceImpl implements ZkRegistryClusterService {
    private static Logger logger = LoggerFactory.getLogger(ZkRegistryClusterServiceImpl.class);

    @Autowired
    private MgrExceptionFactory mgrExceptionFactory;

    @Autowired
    private ZkRegistryService registryService;

    private static final String REGISTRYURL = "/Timer/zkRegistry/autoRegistry";
    
    @Value("${server.port}")
    private long tomcatPort;

    @Override
    public void registerCluster(String clusterInfo) throws UnknownHostException, InterruptedException {
        if (StringUtils.isEmpty(clusterInfo)) {
            mgrExceptionFactory.createException("100000008", "ip:port|ip:port||ip:port|ip:port", "clusterInfo");
        }
        String[] slots = clusterInfo.split("\\|\\|");
        List<String> masters = new ArrayList<>();
        List<String> slaves = new ArrayList<>();
        List<String> tcpPorts = new ArrayList<>();
        for(int i = 0;i < slots.length;++i){
            String[] addresses = slots[i].split("\\|");
            masters.add(addresses[0]);
            for(int j = 1;j < addresses.length;++j){
                slaves.add(addresses[j]);
            }
        }
        
        remoteSend(slots.length, masters,0,tcpPorts);
        remoteSend(slots.length, slaves,1,tcpPorts);
            
    }

    private void remoteSend(int slotsLength, List<String> remotes,int type,List<String> tcpPorts) {
        for(int i = 0;i < remotes.size();++i){
            String result = remoteRegister(remotes.get(i), String.valueOf(i),slotsLength,type);
            if (!result.contains("success")) {
                logger.error("remote register failure, remote server: {}, error: {}", remotes.get(i), result);
                throw new RuntimeException("注册ip" + remotes.get(i) + "失败");
            }
        }
    }

    private String remoteRegister(String uri, String slotCode,int totalSlot,int type) {
        String postString = "{\"ROOT\": {\"BODY\": {\"BUSI_INFO\": {\"type\": "+ type + ",\"totalSlot\": "+ totalSlot + ",\"clusterInfo\": \"\",\"slotCode\":\""
                + slotCode + "\"}}}}";
        if (!uri.contains(":")) {
            logger.error("remote server format error, needed: \"ip:port\", actual: {}", uri);
            return "server format error";
        }

        String url = String.format("%s%s%s", "http://", uri, REGISTRYURL);
        return HttpUtil.synSend(url, HeaderUtil.getJsonMap(), postString);
    }
}