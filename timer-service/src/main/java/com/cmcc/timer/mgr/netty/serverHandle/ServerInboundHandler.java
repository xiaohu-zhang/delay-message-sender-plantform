package com.cmcc.timer.mgr.netty.serverHandle;

import java.nio.file.Paths;

import com.cmcc.timer.mgr.controller.ipresent.FreezeController;
import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.controller.model.ipresent.OpEnum;
import com.cmcc.timer.mgr.init.messageLoader.AddMessageLoader;
import com.cmcc.timer.mgr.netty.SocketRelate;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

    private AddMessageLoader addMessageLoader = new AddMessageLoader();

    private FreezeController freezeController;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 入hashMap
        SocketRelate.allChannels.add(ctx.channel());
        //首先向当前的文件中查询filePostion
        super.channelActive(ctx);
    }
    
    
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 清理socketRelated
        SocketRelate.removeChannel(ctx);
        super.channelInactive(ctx);
    }



    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // TODO Auto-generated method stub
        ByteBuf msgb = (ByteBuf)msg;
        Byte op = msgb.readByte(); 
        switch(op){
        //client端传入发送成功的则将countdownlantch同步下 
        case 2:
        {
            SocketRelate.channelSyncMap.get(ctx.channel().id()).countDown();
            break;
        }
        //获取
        case 4: 
        {
            String slaveNodeId = msgb.toString(CharsetUtil.UTF_8);
            ctx.channel().attr(SocketRelate.remoteUid).set(slaveNodeId);
            SocketRelate.ctxByAdress.put(slaveNodeId, ctx.channel());
            //等到slave向master发送完毕数据才能设置SocketRelate.attributeKey为true
            break;
        }
        case 5:
            /**
             * 接受到的client端作为曾经的server，client多余的日志,这里后续可以根据时间决定直接写文件
             */
        {
            boolean sendOver = msgb.readByte() == 1 ? true : false;
            int index = msgb.readerIndex();
            while (msgb.readableBytes() != 0) {
                int lineSize = msgb.readInt();
                int opCode = msgb.readByte();
                long deadLine = msgb.readLong();
                ByteBuf content = msgb.slice(msgb.readerIndex(), lineSize - 13);
                FreezeModel model = addMessageLoader.getFreezeModel(content, lineSize, deadLine);
                if (opCode == OpEnum.Add.getValue()) {
                    freezeController.autoDeFreeze(model);
                } else if (opCode == OpEnum.Cancel.getValue()) {
                    freezeController.cancleDeFreeze(model);
                }
                index += lineSize;
                msgb.readerIndex(index);
            }
            ByteBuf resieveOver = Unpooled.buffer(5);
            resieveOver.writeInt(1);
            resieveOver.writeByte(6);
            ctx.writeAndFlush(resieveOver, ctx.voidPromise());
            break;
        }
        case 6:
            /**
             * 表示slave已经发送数据完毕，可以由master向slave发送数据了
             */
            ctx.channel().attr(SocketRelate.attributeKey).set(true);
            break;
        }
        
        
        super.channelRead(ctx, msg);
    }

    public void setFreezeController(FreezeController freezeController) {
        this.freezeController = freezeController;
    }
    
    
    
    public ServerInboundHandler(FreezeController freezeController) {
		super();
		this.freezeController = freezeController;
	}



	public static void main(String...strings){
        System.out.println(Paths.get("c:", "x","b").toString());
        
    }

}
