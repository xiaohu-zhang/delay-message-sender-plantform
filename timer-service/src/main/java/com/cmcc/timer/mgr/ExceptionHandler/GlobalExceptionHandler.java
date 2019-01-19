package com.cmcc.timer.mgr.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.cmcc.timer.mgr.controller.model.BaseOutputModel;
import com.cmcc.timer.mgr.controller.model.ThreadLocalModel;
import com.cmcc.timer.mgr.exception.MgrBaseException;

@ControllerAdvice(annotations={RestController.class})
@ResponseBody
public class GlobalExceptionHandler {
    @Autowired
    private MgrExceptionFactory mgrExceptionFactory;
    
    @ExceptionHandler(value={Exception.class})
    public BaseOutputModel defaultRestControllerExceptionhandle(HttpServletRequest req,Exception e){
        MgrBaseException eM;
        if(e instanceof MgrBaseException ){
             eM = (MgrBaseException)e;
        }else if( e.getCause() instanceof MgrBaseException){
            eM = (MgrBaseException)(e.getCause());
        }else if(e instanceof MethodArgumentNotValidException){
            ObjectError oe = ((MethodArgumentNotValidException)e)
                    .getBindingResult().getAllErrors().get(0);
            String fieldErrorMessage = "";
            if(oe instanceof FieldError){
                fieldErrorMessage = ((FieldError)oe).getField().toString();
            }
            eM = mgrExceptionFactory.createException("100000008",oe.getDefaultMessage(),fieldErrorMessage);
        }else{
            eM = mgrExceptionFactory.createException("100000001");
        }
        //301错误属于外部跳转到其他机器进行请求，不作为错误异常进行记录
        if(!eM.getReturnCode().equals("301")){
            logger.error("",e);
        }
        return mgrExceptionFactory.getErrorMap(eM.getNeededMessage(),eM.getReturnCode(),eM.getNeededMessage(),
        		eM.getNeededMessage(),eM.getNeededMessage(),ThreadLocalModel.getThreadLocal().getLocalIp(),
                ThreadLocalModel.threadLocal.get().getTreceId());
    }

 
    
    private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
}
