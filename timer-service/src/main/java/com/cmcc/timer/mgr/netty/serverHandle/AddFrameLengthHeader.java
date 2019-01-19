package com.cmcc.timer.mgr.netty.serverHandle;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class AddFrameLengthHeader extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // TODO Auto-generated method stub
        ByteBuf b = (ByteBuf)msg;
        CompositeByteBuf compbuf = Unpooled.compositeBuffer(2);
        ByteBuf length = Unpooled.copyInt(b.readableBytes());
        compbuf.addComponents(true, length,b);
        super.write(ctx, compbuf, promise);
    }
    
    

}
