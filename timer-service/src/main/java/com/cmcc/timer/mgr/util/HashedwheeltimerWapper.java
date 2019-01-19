package com.cmcc.timer.mgr.util;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jctools.maps.NonBlockingHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.service.store.StoreStrategy;

public class HashedwheeltimerWapper  {
    // 60s一圈的时间轮
    private HashedWheelTimer timer = new HashedWheelTimer(1, TimeUnit.SECONDS);
    
    // 待delay调用任务
    private Map<Object,Timeout> timeoutTaskByPayType = new NonBlockingHashMap<>();
    
    private static HashedwheeltimerWapper wapper = new HashedwheeltimerWapper();
    
    @Autowired
    @Qualifier("cancelSyncByteStoreStrategy")
    private StoreStrategy cancelStoreStrategy;
    
    private HashedwheeltimerWapper(){
    }
    
    public Timeout addTimeout(TimerTask task, long delay, TimeUnit unit,Object ext){
        //根据create_time判断当前操作是否能代替原task
        Timeout preTimeOut = timeoutTaskByPayType.get(ext);
        if(preTimeOut != null){
            long preCreateTime = ((FreezeModel) preTimeOut.getExt()).getCreateTime();
            if(((FreezeModel)ext).getCreateTime() < preCreateTime){
                //replace和cancel的model中的createtime不能比当前传入的createtime早
                return null;
            }
        }
        Timeout timeout = timer.newTimeout(task, delay, unit,ext);
        
        //如果两次发送一模一样的消息，则这里试图取消之前的消息
        Timeout t = timeoutTaskByPayType.put(ext, timeout);
            if (t != null) {
                try {
                    //当前消息属于重复消息，删除当前消息任务,返回当前时间轮消息
                    //如果msg相同，deadline不同，老消息的回调，导致删除新消息的在map中的entry。如果触发gc，则t=null。但
                    //并不影响新消息任务已经在时间轮中，依然会被超时回调。只是，因为map中已经无信息，无法再取消
                    t.cancel();
                } catch (Exception e) {
                    // 这里多线程并发，可能会导致t已经被gc掉，报空异常
                    logger.error("timeout Cacel exception " , e);
                }
            } 
            timeout.task().signal();
            return timeout;
    }
    
    /**
     * 有三种返回值：1 消息时序错误 2 取消失败 3 取消成功 
     * @param ext
     * @return int
     */
    public int cancel(Object ext) {
        boolean isCanceled;
        Timeout preTimeout = timeoutTaskByPayType.get(ext);
        if (preTimeout == null) {
            // task未入hashmap，此时有三种情况:
            // 1. 尚未进入时间轮
            // 2. 已经进入时间轮，但尚未put到hashmap中
            // 3. 未收到该任务的请求
            // 处理方法，直接忽略该请求，因为1,2情况，会触发expire回调事件，在回调中，会删除掉hashmap中的task引用 3情况，不存在的任务直接忽略
            return 2;
        }
        if (preTimeout != null && ((FreezeModel) ext).getCreateTime() < ((FreezeModel) preTimeout.getExt()).getCreateTime()) {
            return 1;
        }
        try {
            isCanceled = preTimeout.cancel();
        } finally {
            timeoutTaskByPayType.remove(ext);
        }
        return isCanceled == true ? 3 : 2;
    }
    
    public static  HashedwheeltimerWapper getInstance(){
        return wapper;
    }

    public Map<Object, Timeout> getTimeoutTaskByPayType() {
        return timeoutTaskByPayType;
    }
    private Logger logger = LoggerFactory.getLogger(HashedwheeltimerWapper.class);
    

}
