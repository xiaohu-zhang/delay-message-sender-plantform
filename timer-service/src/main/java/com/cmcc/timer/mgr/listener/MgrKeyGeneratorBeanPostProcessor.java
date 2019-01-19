package com.cmcc.timer.mgr.listener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * replace the  @Cacheable with  @Cacheable(keyGenerator="mgrKeyGenerator") in order to using mgrKeyGenerator as the dafult keyGenerator
 * 
 * @author silver
 * @see SimpleKeyGenerator
 *
 */
@Component
public class MgrKeyGeneratorBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // TODO Auto-generated method stub
        Object target = bean;
            if(bean instanceof FactoryBean ){
                    try {
                        target = ((FactoryBean)bean).getObject();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        logger.error("error in call getObject,bean name"+beanName,e);
                        throw new RuntimeException(e);
                    }
                
            }
        if (target != null) {
            Set<Class<?>> classes = ClassUtils.getAllInterfacesForClassAsSet(target.getClass());
            classes.add(target.getClass());
            for (Class c : classes) {
                Method[] methods = ReflectionUtils.getAllDeclaredMethods(c);
                for (Method m : methods) {
                    Cacheable[] cacheables = m.getDeclaredAnnotationsByType(Cacheable.class);
                    if (cacheables.length != 0) {
                        for (Cacheable cacheAnnotation : cacheables) {
                            Object cacheAnnotationObject = Proxy.getInvocationHandler(cacheAnnotation);
                            Field valueCacheField = ReflectionUtils.findField(cacheAnnotationObject.getClass(),
                                    "memberValues");
                            valueCacheField.setAccessible(true);
                            LinkedHashMap map = (LinkedHashMap) ReflectionUtils.getField(valueCacheField,
                                    cacheAnnotationObject);
                            if (StringUtils.isEmpty(map.get("keyGenerator"))) {
                                map.put("keyGenerator", "mgrKeyGenerator");
                            }
                        }
                    }
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // TODO Auto-generated method stub
        return bean;
    }

    private Logger logger = LoggerFactory.getLogger(MgrKeyGeneratorBeanPostProcessor.class);
}
