package com.cmcc.timer.mgr.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.cmcc.timer.mgr.validator.TimePatternValidator;


/**
 * 对以字符串形式描述的日期格式进行校验
 * @author silver
 *
 */
@Documented
@Constraint(validatedBy = {TimePatternValidator.class})
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TimePattern {
    String pattern() default "yyyyMMdd HH:mm:ss";
    
    String message() default "{cmcc.validation.constraints.TimePattern.message}";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
    
  //指定多个时使用  
    @Target({ElementType.FIELD})  
    @Retention(RUNTIME)  
    @Documented  
    @interface List {  
        TimePattern[] value();  
    }
}
