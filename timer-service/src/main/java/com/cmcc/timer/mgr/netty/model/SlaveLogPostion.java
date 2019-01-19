package com.cmcc.timer.mgr.netty.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

public class SlaveLogPostion {
    private String fileName;
    private long position;//下次发送的起始位置
    private int suffix;
    
    public SlaveLogPostion(){
        this.suffix = -1;
    }
    
    public SlaveLogPostion(String fileName, long position, int suffix) {
        super();
        this.fileName = fileName;
        this.position = position;
        this.suffix = suffix;
    }

    public SlaveLogPostion(String fileName, long position) {
        super();
        this.fileName = fileName;
        this.position = position;
    }
    
    public ByteBuf toByteBuf(){
        ByteBuf b = Unpooled.buffer();
        byte[] fileNameBytes = fileName.getBytes(CharsetUtil.UTF_8);
        b.writeInt(fileNameBytes.length);
        b.writeBytes(fileNameBytes);
        b.writeLong(position);
        return b;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public int getSuffix() {
        return suffix;
    }

    public void setSuffix(int suffix) {
        this.suffix = suffix;
    }
    
    
    
    
}
