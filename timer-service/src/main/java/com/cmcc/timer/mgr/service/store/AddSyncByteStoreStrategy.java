package com.cmcc.timer.mgr.service.store;

import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.controller.model.ipresent.OpEnum;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

@Service("addSyncByteStoreStrategy")
public class AddSyncByteStoreStrategy extends SyncByteStoreStrategy {
    
    /**单条记录格式:
     * totallength(int)
     * op(byte)
     * deadline(long)
     * createTime(long)
     * topicLen(byte)
     * message(bytes)
     * topic
     */
    @Override
    protected ByteBuf fillWriteBufWithOutLength(FreezeModel model) {
        ByteBuf redoBuf = Unpooled.buffer(128);
        byte[] messageBytes = model.getFreezeSn().getBytes(CharsetUtil.UTF_8);
        byte[] topicBytes = model.getTopic().getBytes(CharsetUtil.UTF_8);
        redoBuf.writeInt(messageBytes.length + topicBytes.length + 22);
        redoBuf.writeByte(OpEnum.Add.getValue());
        redoBuf.writeLong(model.getDeadTime().getTime());
        redoBuf.writeLong(model.getCreateTime());
        redoBuf.writeByte(model.getTopic().length());
        redoBuf.writeBytes(messageBytes);
        redoBuf.writeBytes(topicBytes);
        return redoBuf;
    }
    
}
