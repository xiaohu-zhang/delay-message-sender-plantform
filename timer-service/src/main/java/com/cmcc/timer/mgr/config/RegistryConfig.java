package com.cmcc.timer.mgr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cmcc.timer.mgr.service.zk.ZkRegistryService;
import com.cmcc.timer.mgr.service.zk.impl.ZkRegistryServiceImpl;

@Configuration
@ConfigurationProperties(prefix = "registry")
public class RegistryConfig {
    private String servers;

    @Bean
    public ZkRegistryService registryService() {
        return new ZkRegistryServiceImpl(servers);
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }
}
