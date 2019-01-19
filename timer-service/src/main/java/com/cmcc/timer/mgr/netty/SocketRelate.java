package com.cmcc.timer.mgr.netty;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;

public class SocketRelate {
    //注意在心跳发现断开时，要删除key,key 为NodeId
    public static Map<String,Channel> ctxByAdress = new ConcurrentHashMap<>();
    
    public static Map<ChannelId,CountDownLatch> channelSyncMap = new ConcurrentHashMap<>();
    
    public static ChannelGroup allChannels; 
    
    public static AttributeKey<Boolean> attributeKey = AttributeKey.valueOf("netty.channel");
    
    public static AttributeKey<Integer> channelnoResponseTimes = AttributeKey.valueOf("netty.channel.noResponseTimes");
    
    public static AttributeKey<CountDownLatch> slaveToMasterKey = AttributeKey.valueOf("netty.channel.sTmkey");
    
    public static AttributeKey<String> remoteUid = AttributeKey.valueOf("netty.channel.remoteUid");
    
    public static Channel serverBindChannel;
    
    public static void initChannelGroup(EventExecutor executor){
        allChannels = new DefaultChannelGroup(executor);
    }
    
    public static void removeChannel(ChannelHandlerContext ctx){
        removeChannel(ctx.channel());
    }
    
    public static Optional<String> getNodeIdByChannel(Channel c){
        for(Map.Entry<String, Channel> entry :ctxByAdress.entrySet()){
            if(entry.getValue().id().equals(c.id())){
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }
    
    public static void removeChannel(Channel channel){
        ctxByAdress.remove(channel.attr(remoteUid).get(), channel);
        Channel c = allChannels.find(channel.id());
        if(c != null){
            try {
                c.close().sync();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        CountDownLatch ch = channelSyncMap.get(channel.id());
        if(ch != null){
            channelSyncMap.remove(channel.id());
            ch.countDown();
        }
    }
    
    public static void clear(){
        ctxByAdress.forEach((k,v)->{
          if(v.attr(slaveToMasterKey).get() != null){
              try {
                v.attr(slaveToMasterKey).get().countDown();
            } catch (Exception e) {
                // ignore
            }
          }
        });
        ctxByAdress.clear();
        channelSyncMap.forEach((k,v)->{
            v.countDown();
        });
        channelSyncMap.clear();
        try {
            allChannels.close().sync();
            if(serverBindChannel != null) {
                serverBindChannel.close().sync();
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        serverBindChannel = null;
    }
}
