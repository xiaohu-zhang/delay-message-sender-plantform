package com.cmcc.timer.mgr.init.messageLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.service.RedisClusterService;
import com.cmcc.timer.mgr.util.HashedwheeltimerWapper;

@Component
public class CacelLoadCallBack implements LoadCallBack {

    @Autowired
    private RedisClusterService clusterService;
    
    @Value("${server.port}")
    private String httpPort;
    
    @Override
    public void afterLoadLineInFile(FreezeModel model) {
        // TODO Auto-generated method stub
        int cancelValue = HashedwheeltimerWapper.getInstance().cancel(model);
        if(cancelValue > 1){
            clusterService.delMsgToLocal(model);
        }
    }
    private Logger logger = LoggerFactory.getLogger(CacelLoadCallBack.class);

    
    

}
