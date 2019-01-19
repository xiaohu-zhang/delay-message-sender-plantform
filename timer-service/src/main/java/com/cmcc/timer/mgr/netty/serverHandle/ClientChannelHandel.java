package com.cmcc.timer.mgr.netty.serverHandle;

import com.cmcc.timer.mgr.init.messageLoader.ByteLoadStrategy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
public class ClientChannelHandel extends ChannelInitializer<SocketChannel>{
    
    private String logPath;
    
    private int maxFrameLength; 
    
    private ByteLoadStrategy byteLoadStrategy;
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // TODO Auto-generated method stub
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addFirst(new LengthFieldBasedFrameDecoder(maxFrameLength + 1024 * 1024 * 100,0,4,0,4));//最多每条消息500B，每1024次检查一下文件最大大小。考虑并发情况，最多设置检查的最大文件大小加100M，作为包最大值
        pipeline.addLast(new ClientInboundHandler(logPath,byteLoadStrategy));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        super.channelActive(ctx);
    }

    public ClientChannelHandel(String logPath,int maxframeLength,ByteLoadStrategy byteLoadStrategy) {
        super();
        this.logPath = logPath;
        this.maxFrameLength = maxframeLength;
        this.byteLoadStrategy = byteLoadStrategy;
    }
    
    
    
}
