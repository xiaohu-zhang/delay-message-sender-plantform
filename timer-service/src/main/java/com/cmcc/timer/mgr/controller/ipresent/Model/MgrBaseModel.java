package com.cmcc.timer.mgr.controller.ipresent.Model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MgrBaseModel {
    private static ObjectMapper mapper = new ObjectMapper();
    
    private Map<String,Object> Root = new HashMap<String, Object>();
    private Map<String,Object> Body = new HashMap<String, Object>();
    @JsonIgnore
    private Map<String,Object> BusiInfo = new HashMap<String, Object>();
    @JsonIgnore
    private Map<String,Object> body = new HashMap<String, Object>();
    
    
    public MgrBaseModel(){
        BusiInfo.put("BUSI_INFO", body);
        Body.put("BODY", BusiInfo);
        Root.put("ROOT", Body);
    }
    
    public Map<String,Object> getBody(){
        return body;
    }
    
    
    public String getJasonString() throws JsonProcessingException{
        return mapper.writeValueAsString(getRoot());
    }

    private Map<String, Object> getRoot() {
        return Root;
    }
    
    
}
