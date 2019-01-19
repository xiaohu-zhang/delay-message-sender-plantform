package com.cmcc.timer.mgr.controller.ipresent;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class CacheEntryExample {
    //逐出缓存，用于外部调用
    @CacheEvict(value={"properties"},allEntries=true)
    public  void closeException(){
        
    }
}
