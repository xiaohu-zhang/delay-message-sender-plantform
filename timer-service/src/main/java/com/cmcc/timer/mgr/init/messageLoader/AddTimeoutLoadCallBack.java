package com.cmcc.timer.mgr.init.messageLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.service.RedisClusterService;
import com.cmcc.timer.mgr.service.WheelTimer;
import com.cmcc.timer.mgr.util.Timeout;

@Component
public class AddTimeoutLoadCallBack implements LoadCallBack {

    @Autowired
    private WheelTimer wheelTimer;

    @Autowired
    private RedisClusterService clusterService;

    @Value("${server.port}")
    private String httpPort;
    
    /**
     * 这里首先插入TimeWheel，如果插入成功，则入redis，返回为null说明消息时序不对，不再处理
     */
    @Override
    public void afterLoadLineInFile(FreezeModel expireModel) {
        // TODO Auto-generated method stub
        Timeout hasAdd = wheelTimer.addTimeout(expireModel, 0);
        if(hasAdd != null){
            clusterService.setMsgToLocal(expireModel);
        }
    }

    
    private Logger logger = LoggerFactory.getLogger(AddTimeoutLoadCallBack.class);
}
