package com.cmcc.timer.mgr.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpUtil {
    private static ConnectingIOReactor ioReactor;
    private static PoolingNHttpClientConnectionManager cm ;
    private static CloseableHttpAsyncClient httpclient;
    private static ObjectMapper mapper = new ObjectMapper();
    private static PoolingHttpClientConnectionManager synManger;
    private static CloseableHttpClient synHttpClient;
    private static int soTimeout = 150000;
    static{
        try {
            //异步httpclient线程池
            IOReactorConfig iOReactorConfig =  IOReactorConfig.custom().setConnectTimeout(100 * 1000)
                    .setSoKeepAlive(true).setSoTimeout(soTimeout * 1000).build();
             ioReactor = new DefaultConnectingIOReactor(iOReactorConfig);
             cm = new SubPoolingNHttpClientConnectionManager(ioReactor);
             cm.setMaxTotal(1000);
             cm.setDefaultMaxPerRoute(500);
             ConnectionConfig connectionConfig =   ConnectionConfig.custom().setCharset(Charset.forName("UTF-8")).build();
             cm.setDefaultConnectionConfig(connectionConfig);
             RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10 * 1000).
                     setSocketTimeout(soTimeout * 1000).setConnectionRequestTimeout(10 * 1000).build();
             httpclient = HttpAsyncClients.custom().setConnectionManager(cm).setDefaultRequestConfig(requestConfig).build();
             httpclient.start();
             //同步http线程池
             synManger = new PoolingHttpClientConnectionManager();
             synManger.setDefaultConnectionConfig(connectionConfig);
             synManger.setMaxTotal(1000);
             synManger.setDefaultMaxPerRoute(500);
             SocketConfig synSocketconfig = SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(soTimeout * 1000).build();
             synManger.setDefaultSocketConfig(synSocketconfig);
             synHttpClient = HttpClients.custom().setConnectionManager(synManger).setDefaultRequestConfig(requestConfig).build();
             
        } catch (IOReactorException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException("error creating DefaultConnectingIOReactor");
        }
    }
    
    /**
     * 同步http请求
     * @param url
     * @param HeaderMap
     * @param body
     * @param asyncResponseHandler 此类可以是单例的，因此不需要每次new出来使用
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    
    public static String synSend(String url, Map<String,String> HeaderMap,final String body){
        try {
            logger.info("send url:" + url + " header: " + mapper.writeValueAsString(HeaderMap) +" body:" + body );
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            logger.error("error parse headermap,url is " +url + " body:" + body,e);
        }
        
        HttpPost httpPost = new HttpPost(url);
        for(Entry<String, String> e : HeaderMap.entrySet()){
            httpPost.setHeader(e.getKey(), e.getValue());
        }
        httpPost.setEntity(new StringEntity(body, Charset.forName("UTF-8")));
        CloseableHttpResponse synResponse;
        try {
            synResponse = synHttpClient.execute(httpPost);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException("error in send synrequest",e);
        }
        return consumeSynResponse(url,body,synResponse);
    }
    
    
    
    
    
    /**
     * 
     * @param url
     * @param HeaderMap
     * @param body
     * @param asyncResponseHandler 此类可以是单例的，因此不需要每次new出来使用
     */
    
    public static void asynsend(String url, Map<String,String> HeaderMap,final String body, AsyncResponseHandler asyncResponseHandler
            ,FailedHandler failedHandler){
        try {
            logger.info("send url:" + url + " header: " + mapper.writeValueAsString(HeaderMap) +" body:" + body );
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            logger.error("error parse headermap,url is " +url + " body:" + body,e);
        }
        HttpPost httpPost = new HttpPost(url);
        for(Entry<String, String> e : HeaderMap.entrySet()){
            httpPost.setHeader(e.getKey(), e.getValue());
        }
        httpPost.setEntity(new StringEntity(body, Charset.forName("UTF-8")));
        httpclient.execute(httpPost, new FutureCallback<HttpResponse>(){

            @Override
            public void completed(HttpResponse result) {
                // TODO Auto-generated method stub
                try {
                    consumeAsynResponse(url, body, asyncResponseHandler, result);
                } catch (Exception e) {
                    // 异步处理时不能抛出异常，否则http线程池直接shutdown，不再能使用
                    logger.error("completed response url:" + url + " body:" + body + "error",e);
                    exceptionHandler(url, HeaderMap, body, failedHandler);
                }
            }

            @Override
            public void failed(Exception ex) {
                // TODO Auto-generated method stub
                logger.error("response url:" + url + " body:" + body + "failed",ex);
                exceptionHandler(url, HeaderMap, body, failedHandler);
            }

            private void exceptionHandler(String url, Map<String, String> HeaderMap, final String body,
                    FailedHandler failedHandler) {
                try {
                    if(failedHandler != null){
                        failedHandler.handle(url, HeaderMap, body);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    logger.error("failed Handler response url:" + url + " body:" + body + "error",e);
                }
            }

            @Override
            public void cancelled() {
                // TODO Auto-generated method stub
                logger.error("response url:" + url + " body:" + body + "cancelled");
            }
            
        });
    }
    
    //测试代码
    public static void main(String...strings) throws InterruptedException, ClientProtocolException, IOException, ExecutionException{
        Map<String,String> HeaderMap = new HashMap<String, String>();
        HeaderMap.put("ContentType", "application/json;charset=utf-8");
        String body = " {\"companyId\": \"34589\",\"migusum\":\"223\",\"effectiveTimeBegin\":\"20151221\","
        		+ "\"effectiveTimeEnd\":\"20141211\",\"activityCode\":\"001\"}";
        String body2 = " {\"companyId\": \"34589\",\"migusum\":\"223\",\"effectiveTimeBegin\":\"20151221\","
        		+ "\"effectiveTimeEnd\":\"20141211\",\"activityCode\":\"002\"}";
        asynsend("http://127.0.0.1:8080/MiGuMgr/rs/service/com_sitech_acctmgr_inter_IPresentIssSvc_cfm?tracelog=12345&c=111",HeaderMap,body,(string)->{
           System.out.println("resieve ok1");
        },null);
        System.out.println("才开始第二个");
        asynsend("http://127.0.0.1:8080/MiGuMgr/rs/service/com_sitech_acctmgr_inter_IPresentIssSvc_cfm?tracelog=12348&c=111",HeaderMap,body2,(string)->{
           System.out.println("resieve ok2");
        },null);
        
//        new Thread(()->{
//            System.out.println(synSend("http://www.baidu.com",HeaderMap,body));
//        }).start();
//        new Thread(()->{
//            System.out.println(synSend("http://www.baidu.com",HeaderMap,body2));
//        }).start();
    }
    
    private static String consumeAsynResponse(String url, final String body, AsyncResponseHandler asyncResponseHandler,
            HttpResponse result) {
        String returnString;
        try {
            returnString = EntityUtils.toString(result.getEntity(), "utf-8");
            logger.error("response to url:" + url + " body:" + body + "responseStatus:" + result.getStatusLine().getStatusCode() + "response:" + returnString);
            int statusCode =result.getStatusLine().getStatusCode();
            if(asyncResponseHandler != null){
                //异步消费result
                if(statusCode == 200){
//                    ThreadPool.getThreadPoolExecutor().execute(new AsyncResponseClass(returnString,asyncResponseHandler));
                    return null;//no meaning
                }
                throw new HttpServerErrorException(HttpStatus.valueOf(statusCode),
                		"error in response code at consumeAsynResponse,request url:"+url+" request body:" + body);
            }else{
                //同步消费result
                if(statusCode == 200){
                    return returnString;
                }
                throw new HttpServerErrorException(HttpStatus.valueOf(statusCode),
                		"error in response code at consumeSynResponse,request url:"+url+" request body:" + body);
            }
        } catch (ParseException | IOException e) {
            // TODO Auto-generated catch block
            logger.error("error in complete response",e);
            throw new RuntimeException("error in complete response",e);
        }finally{
            EntityUtils.consumeQuietly(result.getEntity());
        }
    }
    
    private static String consumeSynResponse(String url, final String body, HttpResponse result){
        return consumeAsynResponse(url,body,null,result);
    }
    
    
    public static PoolingNHttpClientConnectionManager getCm() {
        return cm;
    }




    public static PoolingHttpClientConnectionManager getSynManger() {
        return synManger;
    }


    private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    
}
