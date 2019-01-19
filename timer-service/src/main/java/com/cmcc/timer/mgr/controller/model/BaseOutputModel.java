package com.cmcc.timer.mgr.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author silver
 *
 */
@SuppressWarnings("unused")
public class BaseOutputModel {
    private Object OUT_DATA;
    private String RETURN_CODE;
    private String TRACEID;
    private String RUN_IP;

    public static class Builder {
        private String RETURN_CODE = "";
        private String RUN_IP = "";
        private Object OUT_DATA = new NullObject();
        private String TRACEID = "";

        public Builder RETURN_CODE(String val) {
            RETURN_CODE = val;
            return this;
        }

        public Builder RUN_IP(String val) {
            RUN_IP = val;
            return this;
        }

        public Builder OUT_DATA(Object val) {
            OUT_DATA = val;
            return this;
        }
        
        public Builder TRACEID(String val) {
            TRACEID = val;
            return this;
        }

        public BaseOutputModel build() {
            return new BaseOutputModel(this);
        }
    }
    
    private static class NullObject{
        
    }

    private BaseOutputModel(Builder builder) {
        RETURN_CODE = builder.RETURN_CODE;
        RUN_IP = builder.RUN_IP;
        OUT_DATA = builder.OUT_DATA;
        TRACEID = builder.TRACEID;
    }

    @JsonProperty("RETURN_CODE")
    public String getRETURN_CODE() {
        return RETURN_CODE;
    }
    @JsonProperty("RUN_IP")
    public String getRUN_IP() {
        return RUN_IP;
    }
    @JsonProperty("OUT_DATA")
    public Object getOUT_DATA() {
        return OUT_DATA;
    }

    public void setRETURN_CODE(String rETURN_CODE) {
        RETURN_CODE = rETURN_CODE;
    }


    public void setRUN_IP(String rUN_IP) {
        RUN_IP = rUN_IP;
    }

    public void setOUT_DATA(Object oUT_DATA) {
        OUT_DATA = oUT_DATA;
    }
    @JsonProperty("TRACEID")
    public String getTRACEID() {
        return TRACEID;
    }

    public void setTRACEID(String tRACEID) {
        TRACEID = tRACEID;
    }

}
