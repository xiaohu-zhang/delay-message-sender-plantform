package com.cmcc.timer.mgr.netty.serverHandle;

import com.cmcc.timer.mgr.controller.ipresent.FreezeController;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
public class ServerChannelHandel extends ChannelInitializer<SocketChannel>{
    
    private int fileSizeMax;
    
    private FreezeController freezeController;
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // TODO Auto-generated method stub
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addFirst(new LengthFieldBasedFrameDecoder(fileSizeMax+ 1024 * 1024 * 100,0,4,0,4));
        pipeline.addLast(new ServerInboundHandler(freezeController));
    }

    public ServerChannelHandel(int fileSizeMax,FreezeController freezeController) {
        super();
        this.fileSizeMax = fileSizeMax;
        this.freezeController = freezeController;
    }
    
    
}
