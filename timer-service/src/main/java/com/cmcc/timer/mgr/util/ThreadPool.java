package com.cmcc.timer.mgr.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zhangkai
 * @date 2016年5月27日 下午2:18:00
 */
@Component
@ConfigurationProperties(prefix="threadPool")
public class ThreadPool implements InitializingBean{

    private static ThreadPool threadPool = new ThreadPool();
    private static int corePoolSize;
    private static int maximumPoolSize;
    private static long keepAliveTime;
    private static ThreadPoolExecutor threadPoolExecutor;
    
    



    /***/
    public static ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    //	public void exe(Runnable task,Long timeout) throws InterruptedException, ExecutionException, TimeoutException{
    //
    //		
    //		if(timeout == null){
    //			threadPoolExecutor.execute(task);
    //		}else if(timeout < 0){
    //			throw new IllegalArgumentException("Runnable task timeout can't to be lt 0 : timeout " + timeout);
    //		}else{
    //			ExeClass runClass = new ExeClass(task,timeout);
    //			threadPoolExecutor.execute(runClass);
    //		}
    //	}
    //	
    //	private class ExeClass implements Runnable {
    //		Runnable r;
    //		long timeout;
    //		ExeClass(Runnable input,long timeout){
    //			r = input;
    //			this.timeout = timeout;
    //		}
    //		public void run(){
    //			try {
    //				threadPoolExecutor.submit(r).get(timeout,TimeUnit.SECONDS);
    //			} catch (Exception e) {
    //				// TODO 发生异常调用后重新包装为runnable异常向上抛出
    //				e.printStackTrace();
    //				throw new RuntimeException(e);
    //		}
    //	}
    //	}
    /***/


    @Override
    public void afterPropertiesSet() throws Exception {
        // TODO Auto-generated method stub
        threadPoolExecutor = new GThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(maximumPoolSize),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }
    
    
    
    //	public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException{
    //		Runnable r1 = new Runnable() {
    //			
    //			@Override
    //			public void run() {
    //				// TODO Auto-generated method stub
    //				for(;;){
    //					
    //				}
    //			}
    //		};
    //		
    //		Runnable r2 = new Runnable() {
    //			
    //			@Override
    //			public void run() {
    //				// TODO Auto-generated method stub
    //				System.out.println("ok");
    //			}
    //		};
    //		
    //		ThreadPool threadPool = ThreadPool.getInstance();
    //		
    //		try {
    //			threadPool.exe(r1, 10L);
    //		} catch (Exception e) {
    //			// TODO Auto-generated catch block
    //			e.printStackTrace();
    //		}
    //		
    //		System.out.println("f");
    //		threadPool.exe(r2, 1000000L);
    //		threadPool.exe(r2, 1000000L);
    ////		ExecutorService r = Executors.newCachedThreadPool();
    ////		r.submit(r1).get(10,TimeUnit.SECONDS);
    ////		System.out.println("o");
    ////		r.submit(r2).get(10,TimeUnit.SECONDS);
    //
    //	}

}
