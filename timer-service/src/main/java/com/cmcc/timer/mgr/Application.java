package com.cmcc.timer.mgr;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RestController;

import com.cmcc.timer.mgr.config.DataBasePerperties;
import com.cmcc.timer.mgr.config.RedisClusterNodes;
import com.cmcc.timer.mgr.util.TimerUtils;


@RestController
@SpringBootApplication
@EnableConfigurationProperties({DataBasePerperties.class,RedisClusterNodes.class})
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
@Lazy(false)
public class Application extends SpringBootServletInitializer {
    
	private static Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        SpringApplication  p =new SpringApplication(Application.class);
        File x = new File("d.txt");
        Path timerConfig =  Paths.get(x.getAbsoluteFile().toPath().resolveSibling("").resolveSibling("").toString(),"timerMq","config");
        logger.error("timer config dir is " + timerConfig);
        TimerUtils.configPath = timerConfig;
        TimerUtils.init();
        Path path1 = Paths.get(timerConfig.toString(), "application.yaml");
        Path path2 = Paths.get(timerConfig.toString(), "application-" + TimerUtils.configMap.get("spring.profiles") + ".yaml");
		System.setProperty("spring.config.location", path1.toFile().getAbsolutePath() + "," + 
				path2.toFile().getAbsolutePath());	
        p.run(args);
    }
    
    @Override  
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    	File x = new File("d.txt");
    	Path timerConfig =  Paths.get(x.getAbsoluteFile().toPath().resolveSibling("").resolveSibling("").toString(),"timerMq","config");
        logger.error("timer config dir is " + timerConfig);
		TimerUtils.configPath = timerConfig;
		TimerUtils.init();
		Path path1 = Paths.get(timerConfig.toString(), "application.yaml");
		Path path2 = Paths.get(timerConfig.toString(), "application-" + TimerUtils.configMap.get("spring.profiles") + ".yaml");
		System.setProperty("spring.config.location", path1.toFile().getAbsolutePath() + "," + 
				path2.toFile().getAbsolutePath());		
        return application.sources(Application.class);  
    }

}
