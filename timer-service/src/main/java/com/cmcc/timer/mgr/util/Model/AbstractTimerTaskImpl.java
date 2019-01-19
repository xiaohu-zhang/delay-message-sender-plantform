package com.cmcc.timer.mgr.util.Model;

import java.util.concurrent.CountDownLatch;

import com.cmcc.timer.mgr.util.TimerTask;

/**
 * @author silver
 * @see com.cmcc.mgr.util.HashedWheelTimer.HashedWheelTimeout.expire()
 */
public abstract class AbstractTimerTaskImpl implements TimerTask {

    private CountDownLatch latch = new CountDownLatch(1);
    
    @Override
    public void await() throws InterruptedException {
        // TODO Auto-generated method stub
        latch.await();
    }

    @Override
    public void signal() {
        // TODO Auto-generated method stub
        latch.countDown();
    }

}
