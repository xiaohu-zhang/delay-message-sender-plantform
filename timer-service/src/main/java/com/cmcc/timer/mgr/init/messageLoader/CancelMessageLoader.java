package com.cmcc.timer.mgr.init.messageLoader;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CancelMessageLoader extends MessageLoader {

    @Autowired
    @Qualifier("cacelLoadCallBack")
    private CacelLoadCallBack loadCallBack;


    @PostConstruct
    public void setLoadBack() {
        this.loadBack = loadCallBack;
    }


    
}
