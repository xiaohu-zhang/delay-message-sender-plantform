package com.cmcc.timer.mgr.util;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.stereotype.Component;

/**
 * the default spring cache  @Cacheable() using the params as the key , so if their are two methods having the same params with different methods name,
 * and using  @Cacheable on the two methods,methods will share the same key.Of course ,it is not appropriate using the default SimpleKeyGenerator.
 * this KeyGenerator will using classloader_classname,method name,paras as cache key . Using as @Cacheable(keyGenerator="mgrKeyGenerator")
 * @author silver
 *
 */
@Component
public class MgrKeyGenerator extends SimpleKeyGenerator {
    
    @Override
    public Object generate(Object target, Method method, Object... params) {
        return generateKey(target.getClass().getClassLoader().getClass().getName()+"_"+target.getClass().getName(),method.getName(),params);
    }


}
