package com.cmcc.timer.mgr.service.zk;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import com.cmcc.timer.mgr.util.LocalHostUtil;

public class ZkClientExt extends ZkClient {
    private Logger logger = LoggerFactory.getLogger(ZkClientExt.class);

    @Value("${registry.root.path}")
    private String registryPath;
    

    private static final int SESSION_TIMEOUT = 1000;
    
    private static final int CONNECT_TIMEOUT = 5000;    

    private static CountDownLatch latch = new CountDownLatch(1);

    public ZkClientExt(String zkServers) {
        super(zkServers,SESSION_TIMEOUT,CONNECT_TIMEOUT);
        try {
            latch.await();
        } catch (InterruptedException e) {

        }
    }
    
    

    @Override
    public void process(WatchedEvent event) {
        super.process(event);
        if (event.getState() == Event.KeeperState.SyncConnected) {
            latch.countDown();
        }
    }

    /**
     * 获取本机对应的slot节点的路径
     *
     * @return
     * @throws UnknownHostException
     */
    public String getLocalSlotPath() throws UnknownHostException {
        String[] data0Slots = getZkNodeData(registryPath, "||");
        if (data0Slots == null || data0Slots.length == 0) {
            logger.error("get zk register node data from zk error, registry path: {}", registryPath);
            return null;
        }
        //获取本机对应的在zk根节点的注册信息，主要用于判断是否为主服务
        List<String> slots = Arrays.asList(data0Slots);
        String localIp = LocalHostUtil.getLocalIp();
        String localSlot = slots.stream().filter(data0 -> data0.contains(localIp))
                .collect(Collectors.toList()).get(0);
        String slotPath = null;
        if (StringUtils.isEmpty(localSlot)) {
            for (String slot : slots) {
                String[] detail = slot.split("/");
                slotPath = String.format("%s/%s", registryPath, detail[0]);
                String slotData = getZkNodeData(slotPath);
                if (slotData.contains(localIp)) {
                    break;
                }
            }
        }

        return slotPath;
    }

    /**
     * 根据路径和分割符获取zk节点中的数据
     *
     * @param path
     * @param splitTag
     * @return
     */
    public String[] getZkNodeData(String path, String splitTag) {
        String registryData = super.readData(path);
        if(registryData != null){
            return registryData.split(splitTag);
        }else{
            return null;
        }
    }

    public String getZkNodeData(String path) {
        return super.readData(path);
    }
    
    public boolean delNodeIfExist(String partentPath,String nodeId){
        if(!exists(partentPath)){
            return false;
        }
        List<String> children = getChildren(partentPath);
        for(String c:children){
            String data = (String)readData(partentPath + "/" + c, true);
            if(data != null && data.contains(nodeId)){
                delete(partentPath + "/" + c);
                return true;
            }
        }
        return false;
    }
    
}
