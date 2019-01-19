package com.cmcc.timer.mgr.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * @ClassName RedisClusterNodes
 * @Description  集群配置类
 * @author 张安波
 * @date 2017年3月28日 10：53：11
 * @version 2.0.0
 */
@ConfigurationProperties(prefix="redis.cluster")
public class RedisClusterNodes {
    private List<String> nodes = new ArrayList<String>();

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }


    
    
    
}
