package com.cmcc.timer.mgr.validator;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.cmcc.timer.mgr.annotation.TimePattern;

public class TimePatternValidator implements ConstraintValidator<TimePattern,String> {
    private String pattern;
    

    public void initialize(TimePattern constraintAnnotation) {
        // TODO Auto-generated method stub
        pattern = constraintAnnotation.pattern();
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        // TODO Auto-generated method stub
        try {
            DateFormat df = new SimpleDateFormat(pattern);
            Date parseDate = df.parse(value);
            if(df.format(parseDate).equals(value)){
                return true;
            }else{
                return false;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
           return false;
        }
    }
    

}
