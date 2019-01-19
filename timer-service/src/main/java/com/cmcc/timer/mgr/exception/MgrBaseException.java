package com.cmcc.timer.mgr.exception;

import java.lang.reflect.Field;

public class MgrBaseException extends RuntimeException {
    private Exception rootException;
    private String neededMessage;
    private String returnCode;
    
    


    public MgrBaseException(String neededMessage,String returnCode,Exception e){
        this(neededMessage,returnCode);
        this.rootException = e;
    }
    
    public MgrBaseException(String neededMessage, String returnCode) {
        this.neededMessage = neededMessage;
        this.returnCode = returnCode;
        try {
            Field detailMessage = Throwable.class.getDeclaredField("detailMessage");
            detailMessage.setAccessible(true);
            detailMessage.set(this, toString());
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            //need do nothing;
        }
    }

    @Override
    public String toString() {
        return " [rootException=" + rootException + ", neededMessage=" + neededMessage  
                + ", returnCode=" + returnCode + "]";
    }

    public Exception getRootException() {
        return rootException;
    }

    public void setRootException(Exception rootException) {
        this.rootException = rootException;
    }

    public String getNeededMessage() {
        return neededMessage;
    }

    public void setNeededMessage(String neededMessage) {
        this.neededMessage = neededMessage;
    }

    public String getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
    }
    
    
    
}
