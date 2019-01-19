package com.cmcc.timer.mgr.controller.ipresent;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.terracotta.context.annotations.ContextAttribute;

import com.cmcc.timer.mgr.ExceptionHandler.MgrExceptionFactory;
import com.cmcc.timer.mgr.annotation.Cmodel;
import com.cmcc.timer.mgr.annotation.Env;
import com.cmcc.timer.mgr.controller.model.BaseOutputModel;
import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.dao.AcComcontractInfoMapper;

@RestController
public class CacheEntryExampleController {
    @Autowired
    private AcComcontractInfoMapper acComcontractInfoMapper;
    @Autowired
    private CacheEntryExample test;
    @Autowired
    private CacheManager cacheManager;
    
    @RequestMapping(value="/MiGuMgr/rs/service/com_sitech_acctmgr_inter_IPresentIssSvc1_cfm")
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED) 
    public void PresentMoney(@Cmodel  @Validated FreezeModel presentMoneyInputModel/*,BindingResult result*/) throws Exception {
        test.closeException();
    }

    @RequestMapping(value="/MiGuMgr/rs/service/com_sitech_acctmgr_inter_IPresentIssSvc2_cfm")
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED) 
    @Env(values={"mig"})
    public String putGetCacheExample(@Cmodel  @Validated FreezeModel presentMoneyInputModel,@RequestAttribute(value="cModel")String m) throws Exception {
        EhCacheCache cache = (EhCacheCache)(cacheManager.getCache("properties"));
        List<Object> keys = cache.getNativeCache().getKeys();
        for(Object key:keys){
            System.out.println(key);
            System.out.println(cache.getNativeCache().get(key).getObjectValue());
        }
        cache.put("hello", "1");
        for(Object key:cache.getNativeCache().getKeys()){
            System.out.println(key);
        }
        
        
        EhCacheCache cacheClusters = (EhCacheCache)(cacheManager.getCache("propertiesclusters"));
        List<Object> clusterkeys = cacheClusters.getNativeCache().getKeys();
        for(Object key:clusterkeys){
            System.out.println(key);
            System.out.println(cacheClusters.getNativeCache().get(key).getObjectValue());
        }
        cacheClusters.put("hello", "hello");
        for(Object key:cacheClusters.getNativeCache().getKeys()){
            System.out.println(key);
        }
        return "";
    }
    
    
    private Logger logger = LoggerFactory.getLogger(CacheEntryExampleController.class);
}
