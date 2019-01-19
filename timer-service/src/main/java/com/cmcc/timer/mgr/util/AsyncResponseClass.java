package com.cmcc.timer.mgr.util;

import org.apache.http.HttpResponse;

import com.cmcc.timer.mgr.util.AsyncResponseHandler;

public class AsyncResponseClass implements Runnable {
    private String result;
    private AsyncResponseHandler asyncResponseHandler;
    
    
    @Override
    public void run() {
        // TODO Auto-generated method stub
        asyncResponseHandler.handleResponse(result);
    }

    public AsyncResponseClass(String result,AsyncResponseHandler asyncResponseHandler) {
        super();
        this.result = result;
        this.asyncResponseHandler = asyncResponseHandler;
    }
    
    

}
