package com.cmcc.timer.mgr.util;
/**
 * @ClassName AsyncResponseHandler
 * @Description  http请求回调接口
 * @author 张安波
 * @date 2017年3月28日 10：53：11
 * @version 2.0.0
 */
@FunctionalInterface
public  interface AsyncResponseHandler{
	/**/
    public abstract void handleResponse(String result);
    
}
