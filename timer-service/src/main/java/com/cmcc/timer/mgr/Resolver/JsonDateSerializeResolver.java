package com.cmcc.timer.mgr.Resolver;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.boot.jackson.JsonObjectDeserializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JsonDateSerializeResolver extends  JsonSerializer<Date>{
        private String pattern = "yyyyMMdd HH:mm:ss";
        
        //设置默认构造函数，防止通过spring boot自动构建这个类
        public JsonDateSerializeResolver(){
            
        }
        
        public JsonDateSerializeResolver(String pattern) {
            super();
            this.pattern = pattern;
        }

        @Override
        public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException, JsonProcessingException {
            // TODO Auto-generated method stub
             gen.writeString(new SimpleDateFormat(pattern).format(value));
        }

        
       
}
