package com.cmcc.timer.mgr.Resolver;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.management.RuntimeErrorException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonObjectDeserializer;

import com.cmcc.timer.mgr.ExceptionHandler.MgrExceptionFactory;
import com.cmcc.timer.mgr.exception.MgrBaseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

public class JsonDateDeserializeResolver extends  JsonObjectDeserializer<Date>{
        private String pattern = "yyyyMMdd HH:mm:ss";
        
        private MgrExceptionFactory mgrExceptionFactory;
        
        //设置默认构造函数，防止通过spring boot自动构建这个类
        public JsonDateDeserializeResolver(){
            
        }
        
        public JsonDateDeserializeResolver(String pattern) {
            super();
            this.pattern = pattern;
        }



        @Override
        protected Date deserializeObject(JsonParser jsonParser, DeserializationContext context, ObjectCodec codec,
                JsonNode tree) throws IOException {
            // TODO Auto-generated method stub
           String inputDateString = tree.asText(pattern);
            try {
                 DateFormat df = new SimpleDateFormat(pattern);
                 Date parsedDate = df.parse(inputDateString);
                 if(!df.format(parsedDate).equals(inputDateString)){
                    throw mgrExceptionFactory.createException("100000008",new String[]{pattern,inputDateString});
                 }
                         
                 return parsedDate;
            } catch (MgrBaseException e) {
                // TODO Auto-generated catch block
                throw e;
            } catch(Exception e){
                throw mgrExceptionFactory.createException("100000001",new String[]{pattern,inputDateString});
            }
        }

        public void setMgrExceptionFactory(MgrExceptionFactory mgrExceptionFactory) {
            this.mgrExceptionFactory = mgrExceptionFactory;
        }
        
        
       
}
