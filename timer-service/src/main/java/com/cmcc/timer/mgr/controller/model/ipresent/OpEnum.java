package com.cmcc.timer.mgr.controller.model.ipresent;

public enum OpEnum {
    Add(1),Cancel(0);
    private int value;
    private OpEnum(int value){
        this.value = value;
    }
    public int getValue(){
        return value;
    }
    
}
