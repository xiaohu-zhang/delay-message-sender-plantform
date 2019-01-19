package com.cmcc.timer.mgr.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cmcc.timer.mgr.controller.model.BaseOutputModel;

@ResponseBody
@Controller
public class DefaultExceptionHandler implements ErrorController {

    @Autowired
    private MgrExceptionFactory mgrExceptionFactory;
    
    //tomcat默认错误处理请求页面地址
    private static final String ERROR_PATH = "/error";
    
    
   
    @Override
    public String getErrorPath() {
        // TODO Auto-generated method stub
        return ERROR_PATH;
    }
    
    @RequestMapping(value=ERROR_PATH)  
    public BaseOutputModel handleError(HttpServletRequest req,HttpServletResponse rsp){  
        String traceId = (String) req.getAttribute("traceId");
        return mgrExceptionFactory.getDefaultErrorMap(traceId);
    }
    
 
    

}
