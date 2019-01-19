package com.cmcc.timer.mgr.service.store.backlog;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.init.messageLoader.ByteLoadStrategy;
import com.cmcc.timer.mgr.netty.SocketRelate;
import com.cmcc.timer.mgr.netty.serverHandle.ClientChannelHandel;
import com.cmcc.timer.mgr.service.zk.ZkClientExt;
import com.cmcc.timer.mgr.util.TimerUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

@Service("backLogStoreClient")
public class BackLogStoreClient {
    private Logger logger = LoggerFactory.getLogger(BackLogStoreClient.class);

    @Value("${registry.servers}")
    private String zkServers;

    @Value("${registry.root.path}")
    private String registryPath;
    
    @Value("${data.path}")
    private String logPath;
    
    @Value("${redoFileSize}")
    int fileSizeMax;
    
    @Autowired
    private ByteLoadStrategy byteLoadStrategy;

    private ZkClientExt zkClientExt;

    private String masterServer;
    
    private String masterNodeId;

    private Bootstrap bootstrap;

    private SocketChannel channel;

    private static CountDownLatch slotNodeListenerLatch = new CountDownLatch(1);
    
    private EventLoopGroup group = new NioEventLoopGroup();

    /**
     * 根据slave的信息启动客户端连接
     *
     * @param masterServer
     * @throws Exception
     */
    public void start(String masterServer,String masterNodeId) {
        String[] hostInfo = masterServer.split(":");
        bootstrap = new Bootstrap();
        try {
            bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000).
            option(ChannelOption.TCP_NODELAY, true).remoteAddress(
                    new InetSocketAddress(hostInfo[0], Integer.parseInt(hostInfo[1])))
                    .handler(new ClientChannelHandel(logPath,fileSizeMax,byteLoadStrategy));
            ChannelFuture future = bootstrap.connect().sync();
            if (future.isSuccess()) {
                channel = (SocketChannel) future.channel();
                channel.attr(SocketRelate.remoteUid).set(masterNodeId);
                SocketRelate.allChannels.add(channel);
                SocketRelate.ctxByAdress.put(masterNodeId, channel);
            }
        } catch (Exception e) {
            logger.error("ServerBootstrap interrupted exception: {}", e);
            throw new RuntimeException(e);
        }
        this.masterServer = masterServer;
        this.masterNodeId = masterNodeId;
        TimerUtils.masterId = masterNodeId;
//        setSlotNodeListener();
//
//        latch.countDown();
    }

    /**
     * 设置该服务节点对应的slot节点的数据监听
     */
//    public void setSlotNodeListener() throws UnknownHostException, InterruptedException {
//        //建立连接
//        initZkClient();
//        //获取本机对应的slot路径
//        String slotPath = zkClientExt.getLocalSlotPath();
//        //针对本机对应的slot设置listener
//        setSlotNodeListener(slotPath);
//        slotNodeListenerLatch.await();
//    }

    /**
     * 建立连接
     */
    @PostConstruct
    public void initZkClient() {
        zkClientExt = new ZkClientExt(zkServers);
        SocketRelate.initChannelGroup(group.next());
    }

    /**
     * 为slot节点设置数据变更的Listener
     * 主要用于服务节点下子节点变更后，更新了该slot节点的数据，然后将变更的数据更新到根节点中
     *
     * @param slotPath
     */
//    private void setSlotNodeListener(String slotPath) {
//        zkClientExt.subscribeDataChanges(slotPath, new IZkDataListener() {
//            @Override
//            public void handleDataChange(String dataPath, Object o) throws Exception {
//                updateMasterServer(slotPath);
//            }
//
//            @Override
//            public void handleDataDeleted(String dataPath) throws Exception {
//                updateMasterServer(slotPath);
//            }
//        });
//        slotNodeListenerLatch.countDown();
//    }
//
//    /**
//     * 监听slot节点的数据变化，当数据发生变化时，与上次结果对比，新的节点需要重新调用start
//     * 用于slave节点发生变化
//     *
//     * @param slotPath
//     */
//    private void updateMasterServer(String slotPath) throws Exception {
//        String[] serverNodes = zkClientExt.getZkNodeData(slotPath, "||");
//        if (!this.masterServer.equals(serverNodes[0])) {
//            this.masterServer = serverNodes[0];
//            CountDownLatch latch = new CountDownLatch(1);
//            start(serverNodes[0], latch);
//        }
//    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public String getMasterServer() {
        return masterServer;
    }

    public void setMasterServer(String masterServer) {
        this.masterServer = masterServer;
    }

    public String getMasterNodeId() {
        return masterNodeId;
    }

    public void setMasterNodeId(String masterNodeId) {
        this.masterNodeId = masterNodeId;
    }

    public EventLoopGroup getGroup() {
        return group;
    }
    
    
}