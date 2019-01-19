package com.cmcc.timer.mgr.util;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cmcc.timer.mgr.dao.AcComcontractInfoMapper;
import com.cmcc.timer.mgr.util.HttpUtil;

@Component
public class FrameWorkScheduler {
    	/** 
	 * tomcat 默认20s 关闭空闲的socket连接(当keepalive相关配置未配置时，由connectiontimeou数值确定)，但是此时client可能connect还是认为是正常的，因此，当server端对应的状态为Fin_wait2的时候，
	 * client从outputstream中读取数据，会抛出异常（当server端为close_wait状态时，不影响，因为此时client代码在BHttpConnectionBase 
	 * isStale 方法返回false，之后client端会关闭socket连接，然后重新开启新的connection）。
	 * 为解决以上问题，将空闲时间设置为15s以上则回收连接,但是注意需要20/2=10内就要做一次检查，因要保证20s超时时间内，有至少两次的监控逻辑。这样才能保证20s超时服务端断开前，一定监控
	 * 程序运行过，在客户端删除了超过15s空闲的连接。
	 * */
	@Scheduled(fixedRate = 8 * 1000)
	public void closeIdleConnections() throws InterruptedException {
		//HttpUtil.getCm().closeIdleConnections(15, TimeUnit.SECONDS);
		HttpUtil.getSynManger().closeIdleConnections(15, TimeUnit.SECONDS);
	}
    
    @Autowired
    private AcComcontractInfoMapper acComcontractInfoMapper;

    public void test() throws InterruptedException{
        System.out.println("test:"+Thread.currentThread().getName());
        System.out.println(String.valueOf(acComcontractInfoMapper.select1()));
    }

    
    
}
