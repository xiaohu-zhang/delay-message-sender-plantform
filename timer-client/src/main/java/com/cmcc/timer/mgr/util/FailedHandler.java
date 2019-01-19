package com.cmcc.timer.mgr.util;

import java.util.Map;

@FunctionalInterface
public  interface FailedHandler{
    public abstract void handle(String url, Map<String,String> HeaderMap,final String body);
    
}
