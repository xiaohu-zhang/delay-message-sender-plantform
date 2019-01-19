package com.cmcc.timer.mgr.controller.tools;

import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cmcc.timer.mgr.annotation.Cmodel;
import com.cmcc.timer.mgr.controller.model.ReloadModel;
import com.cmcc.timer.mgr.init.Starter;
import com.cmcc.timer.mgr.netty.SocketRelate;
import com.cmcc.timer.mgr.service.zk.impl.ZkRegistryServiceImpl;
import com.cmcc.timer.mgr.util.TimerUtils;

@RestController
@RequestMapping("/tools")
public class Tools {
    
    @Autowired
    private Starter starter;

    @Value("${data.path}")
    private String dataPath;
    
    @Autowired
    private ZkRegistryServiceImpl zkRegistryServiceImpl;
    
    @RequestMapping(value = "/reload")
    public String autoDeFreeze(@Cmodel @Validated ReloadModel reload) throws Exception {
        starter.load0(reload.getBeginTime().getTime(), Long.MAX_VALUE,reload.getEndTime(),null);
        return "success";
    }
    
    /**
     * clear 全部的注册信息，清空节点注册及日志等全部内容。例如需要重新注册，配置主备等，可以使用此方法
     * @throws InterruptedException 
     * @throws IOException 
     */
    @RequestMapping(value = "/clear")
    public String clear() throws InterruptedException, IOException{
        TimerUtils.currentNodetype = 1;
        SocketRelate.clear();
        TimerUtils.rMDeleteFile(Paths.get("/data"));
        if(TimerUtils.slot >= 0){
            zkRegistryServiceImpl.getZkClientExt().delNodeIfExist("/timer/"+TimerUtils.slot+"/freezeservice", TimerUtils.getNodeId());
        }
        zkRegistryServiceImpl.getZkClientExt().delNodeIfExist("/monitor", TimerUtils.getNodeId());
        TimerUtils.masterId = null;
        return "success";
    }
    
    public static void main(String...strings){
        System.out.println("/data".replaceAll("/", "\\\\"));
    }

    private Logger logger = LoggerFactory.getLogger(Tools.class);


}
