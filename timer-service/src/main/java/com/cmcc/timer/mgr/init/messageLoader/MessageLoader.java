package com.cmcc.timer.mgr.init.messageLoader;

import java.nio.ByteBuffer;
import java.util.Date;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

public abstract class MessageLoader {
    // 多load更改时间1分钟内的过期数据，这是为了防止在写入redolog的时候，该发送的过期消息没有发送
    long moreLoadBeforeSeconds = 1 * 60 * 1000;
    
    protected int preparedMsgBufSize = 1024;

    protected ByteBuf msg = Unpooled.wrappedBuffer(ByteBuffer.allocate(preparedMsgBufSize));
    
    protected ByteBuf topic = Unpooled.buffer(128);
    
    protected LoadCallBack loadBack;
    
    public void load(ByteBuf buf,int lineSize,long lastWriteTime,long maxModifyTime){
        long deadLineTime = buf.readLong();
        if (deadLineTime > lastWriteTime - moreLoadBeforeSeconds
                && deadLineTime < maxModifyTime){
         // 只有过期时间在最后写入时间之后的数据，我们才需要重新load
            FreezeModel expireModel = getFreezeModel(buf,lineSize,deadLineTime);

            loadBack.afterLoadLineInFile(expireModel);
        }
    }

    public  FreezeModel getFreezeModel(ByteBuf buf, int lineSize,long deadLineTime){
        FreezeModel f = new FreezeModel();
        long createTime = buf.readLong();
        int topicLen = buf.readByte();
        
        int messageBytes = getMessageBytes(lineSize) - topicLen;
        if (messageBytes > preparedMsgBufSize) {
            msg = Unpooled.wrappedBuffer(ByteBuffer.allocate(messageBytes));
        }
        //略过create_time等字段
        msg.clear();
        topic.clear();
        buf.readBytes(msg, messageBytes);
        buf.readBytes(topic,topicLen);
        f.setFreezeSn(msg.toString(CharsetUtil.UTF_8));
        f.setTopic(topic.toString(CharsetUtil.UTF_8));
        f.setDeadTime(new Date(deadLineTime));
        f.setCreateTime(createTime);
        long exprieTime = (deadLineTime - System.currentTimeMillis()) / 1000;
        f.setDelayTime(exprieTime);
        return f;
    }


    public  int getMessageBytes(int linesize){
            // TODO Auto-generated method stub
            return linesize - 22;
    }
    
}
