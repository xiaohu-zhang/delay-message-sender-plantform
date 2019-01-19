package com.cmcc.timer.mgr.controller.ipresent;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cmcc.timer.mgr.annotation.Cmodel;
import com.cmcc.timer.mgr.controller.model.ipresent.ZkRegistryModel;
import com.cmcc.timer.mgr.service.zk.ZkRegistryClusterService;
import com.cmcc.timer.mgr.service.zk.ZkRegistryService;

@RestController
@RequestMapping("/zkRegistry")
public class ZkRegistryController {

    @Autowired
    private ZkRegistryService registryService;

    @Autowired
    private ZkRegistryClusterService registryClusterService;

    //信息的格式为：172.28.20.100:8080|172.28.20.101:8080||172.28.20.200:8080|172.28.20.201:8080
    @RequestMapping(value = "/registryCluster")
    public String registryCluster(@Cmodel @Validated ZkRegistryModel registryModel) throws UnknownHostException,
            InterruptedException {
        registryClusterService.registerCluster(registryModel.getClusterInfo());

        return "success";
    }

    @RequestMapping(value = "/autoRegistry")
    public String registryService(@Cmodel @Validated ZkRegistryModel registryModel) throws UnknownHostException {
        registryService.register(registryModel.getSlotCode(),registryModel.getTotalSlot());

        return "success";
    }
}