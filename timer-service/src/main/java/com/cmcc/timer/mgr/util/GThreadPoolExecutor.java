package com.cmcc.timer.mgr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangkai
 * @date 2016年5月27日 下午2:18:00
 */
public class GThreadPoolExecutor extends ThreadPoolExecutor {
    /***/
    public GThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                               TimeUnit unit, BlockingQueue<Runnable> workQueue,
                               RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    /***/
    @Override
    public void shutdown() {
        // 因为是全局线程池，不允许执行shutdown操作 

    }

    /***/
    @Override
    public List<Runnable> shutdownNow() {
        // TODO Auto-generated method stub
        // 因为是全局线程池，不允许执行shutdown操作 
        return new ArrayList<Runnable>();
    }

}
