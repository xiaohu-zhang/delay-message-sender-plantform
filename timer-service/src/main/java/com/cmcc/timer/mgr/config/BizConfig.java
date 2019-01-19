package com.cmcc.timer.mgr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.cmcc.timer.mgr.service.zk.ZkClientExt;

@Configuration
@EnableCaching
@ImportResource(locations={"classpath:quartzJob.xml"})
public class BizConfig {
    @Value("${registry.servers}")
    private String zkServers;
    
    @Bean  
    public ZkClientExt  generateZkClientExt() {  
        return new ZkClientExt(zkServers);  
    }  
    


    
    
}
