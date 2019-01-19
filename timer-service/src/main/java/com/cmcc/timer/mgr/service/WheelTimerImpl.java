package com.cmcc.timer.mgr.service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.controller.ipresent.Model.MgrBaseModel;
import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.util.CUUID;
import com.cmcc.timer.mgr.util.HashedwheeltimerWapper;
import com.cmcc.timer.mgr.util.HeaderUtil;
import com.cmcc.timer.mgr.util.HttpUtil;
import com.cmcc.timer.mgr.util.ThreadPool;
import com.cmcc.timer.mgr.util.Timeout;
import com.cmcc.timer.mgr.util.TimerUtils;
import com.cmcc.timer.mgr.util.Model.AbstractTimerTaskImpl;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class WheelTimerImpl implements WheelTimer {
    
    private int[] failedDelayTimes;

    @Value("${spring.profiles.active}")
    private String activeYamlSuffix;
    
    @Value("${deFreezeUrl}")
    private String deFreezeUrl;
    
    @Value("${failedRetryTimes}")
    private String failedRetryTimes;
    
    public Timeout addTimeout(FreezeModel presentMoneyInputModel, int tryTimes) {
         return  HashedwheeltimerWapper.getInstance().addTimeout(new AbstractTimerTaskImpl() {
            @Override
            public void run(Timeout t) throws Exception {
                if(TimerUtils.currentNodetype == 1){
                    //slave
                    return;
                }
                // TODO Auto-generated method stub
                ThreadPool.getThreadPoolExecutor().execute(()->{
                    try {
                        expireTask(tryTimes, t);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        logger.error("error in expireTask: " + t.getExt().toString(),e);
                    }
                });
            }

            private void expireTask(int tryTimes, Timeout t) throws JsonProcessingException {
                // 返回的Timeout  可能为null 原因是
                // 发送两笔一抹一眼的msg，第一笔完成后已经删除了这个msg，第二笔则无法从mashmap中取到msg了
                // 也有可能删除了新增加的deadline不同的timeout，这种情况属于bug
                HashedwheeltimerWapper.getInstance().getTimeoutTaskByPayType().remove(t.getExt());
                MgrBaseModel m = new MgrBaseModel();
                m.getBody().put("transactionId", CUUID.generateUID());
                m.getBody().put("freezeSn", ((FreezeModel) t.getExt()).getFreezeSn());
                HttpUtil.asynsend(deFreezeUrl,
                        HeaderUtil.getJsonMap(), m.getJasonString(), (r) -> {
                            // 返回200，即认为发送成功
                        }, (url, map, body) -> {
                            // 调用失败，重试
                            int trytimestemp = tryTimes;
                            if(trytimestemp >= failedDelayTimes.length){
                                failedMegLogger
                                .error("send " + trytimestemp + " failed, info: " + presentMoneyInputModel);
                            }else{
                                presentMoneyInputModel.setDelayTime(failedDelayTimes[trytimestemp]);
                                addTimeout(presentMoneyInputModel, ++trytimestemp);
                            }
                        });
            }
        }, presentMoneyInputModel.getDelayTime(), TimeUnit.SECONDS, presentMoneyInputModel);
    }
    
    @PostConstruct
    public void setFailedDelayTimes() { 
        failedDelayTimes = Arrays.asList(failedRetryTimes.split(","))
                .stream().mapToInt(v -> Integer.valueOf(v)).toArray();
    }
    
    private Logger failedMegLogger = LoggerFactory.getLogger("failedMessage");
    private Logger logger = LoggerFactory.getLogger(WheelTimerImpl.class);
}
