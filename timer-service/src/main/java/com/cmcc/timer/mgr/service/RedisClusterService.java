package com.cmcc.timer.mgr.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.ExceptionHandler.MgrExceptionFactory;
import com.cmcc.timer.mgr.controller.model.ThreadLocalModel;
import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;
/**
 * @ClassName RedisClusterService
 * @Description  集群配置类
 * @author 张安波
 * @date 2017年3月28日 10：53：11
 * @version 2.0.0
 */
@Service
public class RedisClusterService {
    @Autowired
    private JedisCommands jc;

    private ThreadLocal<ThreadLocalModel> threadLocal = ThreadLocalModel.threadLocal;
    
    private DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    @Autowired
    private MgrExceptionFactory expFactory;
    
    //将当前的(messageKey,deadtime)放入rediscluster中，用作后续消息的取消，这里使用原子操作，是为了保证redis中key都是有过期时间的
    public String setMsgToLocal(FreezeModel model){
        
        String setResutle = ((JedisCluster)jc).set(model.redisKey(), LocalDateTime.ofInstant(model.getDeadTime().toInstant(),ZoneId.systemDefault()).format(df), "NX", "EX", 
                model.getDelayTime() > 0 ? model.getDelayTime() : 1);
        if(!"OK".equals(setResutle)){
            setResutle = ((JedisCluster)jc).set(model.redisKey(), LocalDateTime.ofInstant(model.getDeadTime().toInstant(),ZoneId.systemDefault()).format(df), "XX", "EX", 
                    model.getDelayTime() > 0 ? model.getDelayTime() : 1);
        }
        if(!"OK".equals(setResutle)){
            logger.error("set to redis with " + model +" error. setResult is " + setResutle) ;
        }
        return setResutle;
    }
    
    public long delMsgToLocal(FreezeModel model){
       return  ((JedisCluster)jc).del(model.redisKey());
    }
    
    public LocalDateTime get(FreezeModel model){
        String deadTime = jc.get(model.redisKey());
        if(null != deadTime ){
           return LocalDateTime.parse(deadTime, df);
          }
        throw expFactory.createException("100000002","消息不存在");
    }

    private Logger logger = LoggerFactory.getLogger(RedisClusterService.class);
}
