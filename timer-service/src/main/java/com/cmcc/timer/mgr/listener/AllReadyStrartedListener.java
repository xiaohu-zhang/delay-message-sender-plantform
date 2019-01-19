package com.cmcc.timer.mgr.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.cmcc.timer.mgr.init.Starter;


@Component
public class AllReadyStrartedListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private Starter starter;
    
    private Logger logger = LoggerFactory.getLogger(AllReadyStrartedListener.class);
    
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("start successfully ...");
    }
    

}
