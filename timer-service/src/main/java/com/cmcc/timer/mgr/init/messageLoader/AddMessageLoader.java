package com.cmcc.timer.mgr.init.messageLoader;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AddMessageLoader extends MessageLoader {

    @Autowired
    @Qualifier("addTimeoutLoadCallBack")
    private AddTimeoutLoadCallBack loadCallBack;
    
    
    @PostConstruct
    public void setLoadBack() {
        this.loadBack = loadCallBack;
    }


}
