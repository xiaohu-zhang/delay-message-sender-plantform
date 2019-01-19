package com.cmcc.timer.mgr.ExceptionHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.cmcc.timer.mgr.controller.model.BaseOutputModel;
import com.cmcc.timer.mgr.exception.MgrBaseException;
import com.cmcc.timer.mgr.util.CacheUtil;

@Component
public class MgrExceptionFactory {
    Properties p ;
    
    @Autowired
    CacheUtil cacheUtil;
    
    public MgrExceptionFactory() throws IOException{
       p =  PropertiesLoaderUtils.loadAllProperties("error.properties");
    }
    
        public  MgrBaseException createException(String excepitonId,String...params ){
            String messageTemplate = cacheUtil.getmessageTemplate(p, excepitonId);
            String message = parseMessageWithPlaceHolder(messageTemplate,params);
            return new MgrBaseException(message,excepitonId);
        }

        
    private String parseMessageWithPlaceHolder(String messageTemplate, String... params) {
        for (int placeHolderIndex = 0; placeHolderIndex < params.length; ++placeHolderIndex) {
            if (!StringUtils.isEmpty(params[placeHolderIndex])) {
                params[placeHolderIndex] = Matcher.quoteReplacement(params[placeHolderIndex]);
                messageTemplate = messageTemplate.replaceAll("!\\{" + placeHolderIndex + "}",
                        params[placeHolderIndex]);
            }
        }
        List<String> messageSegments = Arrays.asList(messageTemplate.split("#"));
        Iterator<String> iterator = messageSegments.iterator();
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNext()) {
            String segi = iterator.next();
            if (segi.contains("!")) {
                continue;
            }
            sb.append(segi);
        }
        return sb.toString();

    }
        
        //内部使用方法
        public BaseOutputModel getDefaultErrorMap(String traceId){

            return getErrorMap("内部错误","100000001","内部错误","内部错误","内部错误","127.0.0.1",traceId);
        }
        
        
        public BaseOutputModel getErrorMap(String... Message) {
            BaseOutputModel mo = new BaseOutputModel.Builder()
                    .OUT_DATA(Message[0])
                    .RETURN_CODE(Message[1])
                    .RUN_IP(Message[5])
                    .TRACEID(Message[6])
                    .build();
            return mo;
        }
        

        
        private Logger logger = LoggerFactory.getLogger(MgrExceptionFactory.class);
    

}
