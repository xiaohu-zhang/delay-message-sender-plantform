package com.cmcc.timer.mgr.service.store;

import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.controller.model.ipresent.OpEnum;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

@Service("cancelSyncByteStoreStrategy")
public class CancelSyncByteStoreStrategy extends SyncByteStoreStrategy {

    /**单条记录格式:
     * totallength(int)
     * op_type(1 byte)
     * deadline(long)
     * createTime(long)
     * topicLen(byte)
     * message(bytes)
     * topic(bytes)
     */
    @Override
    protected ByteBuf fillWriteBufWithOutLength(FreezeModel model) {
        // TODO Auto-generated method stub
        ByteBuf redoBuf = Unpooled.buffer(128);
        byte[] messageBytes = model.getFreezeSn().getBytes(CharsetUtil.UTF_8);
        byte[] topicBytes = model.getTopic().getBytes(CharsetUtil.UTF_8);
        redoBuf.writeInt(messageBytes.length + topicBytes.length + 22);//22 =  4 + 1 + 8 + 8 + 1
        redoBuf.writeByte(OpEnum.Cancel.getValue());
        redoBuf.writeLong(model.getDeadTime().getTime());
        redoBuf.writeLong(model.getCreateTime());
        redoBuf.writeByte(model.getTopic().length());
        redoBuf.writeBytes(messageBytes);
        redoBuf.writeBytes(topicBytes);
        return redoBuf;
    }
    
    
}
