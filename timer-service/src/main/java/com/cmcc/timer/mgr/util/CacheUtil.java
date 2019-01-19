package com.cmcc.timer.mgr.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cmcc.timer.mgr.Resolver.JsonDateDeserializeResolver;
import com.cmcc.timer.mgr.Resolver.JsonDateSerializeResolver;
import com.cmcc.timer.mgr.exception.MgrBaseException;

@Component
@CacheConfig(cacheNames = "properties")
public class CacheUtil {

        
        @Cacheable(keyGenerator="mgrKeyGenerator")
        public  JsonDateDeserializeResolver getJsonDateDeserializeResolver(String pattern){
            return new JsonDateDeserializeResolver(pattern);
        }
        
        @Cacheable(keyGenerator="mgrKeyGenerator")
        public  JsonDateSerializeResolver getJsonDateSerializeResolver(String pattern){
            return new JsonDateSerializeResolver(pattern);
        }
        
        @Cacheable(keyGenerator="mgrKeyGenerator")
        public boolean check(String[] strings,String target){
            if(strings == null){
                if(target == null){
                    return true;
                }
                return false;
            }
            for(String e:strings){
                if(e.equals(target)){
                    return true;
                }
            }
            return false;
        }
        
        @Cacheable(keyGenerator="mgrKeyGenerator")
        public String getmessageTemplate(Properties p,String excepitonId){
            String messageTemplate = p.getProperty(excepitonId,p.getProperty("100000001"));
            messageTemplate = messageTemplate.replaceAll("\\$", "!");
            return messageTemplate;
        }
}
