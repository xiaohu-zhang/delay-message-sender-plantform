package com.cmcc.timer.mgr.util;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class SpringContextUtil /*implements ServletContextListener*/ {

    private static List<String> serviceLists = new ArrayList<>();

//    @Override
//    public void contextInitialized(ServletContextEvent servletContextEvent) {
//        //获取请求映射
//        ServletContext servletContext = servletContextEvent.getServletContext();
//        ApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
//        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
//        Map<RequestMappingInfo, HandlerMethod> infoMap = mapping.getHandlerMethods();
//        for (RequestMappingInfo info : infoMap.keySet()) {
//            if(!StringUtils.isEmpty(info.getName())) {
//                serviceLists.add(info.getName());
//            }
//        }
//    }

//    @Override
//    public void contextDestroyed(ServletContextEvent servletContextEvent) {
//    }

    public List<String> getServiceLists() {
        return serviceLists = Arrays.asList("freezeservice");
    }

    public void setServiceLists(List<String> serviceLists) {
        SpringContextUtil.serviceLists = serviceLists;
    }
}
