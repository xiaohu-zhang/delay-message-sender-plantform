package com.cmcc.timer.mgr.service.store.backlog;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service("backLogStoreStrategy")
public class BackLogStoreStrategy {
    private Logger logger = LoggerFactory.getLogger(BackLogStoreStrategy.class);

    @Autowired
    private BackLogStoreClient storeClient;

    private static LinkedBlockingQueue<ByteBuf> backLogQueue = new LinkedBlockingQueue();

    /**
     * 将数据放入queue中
     *
     * @param buf
     */
    public void addBackLog(ByteBuf buf) {
        try {
            this.backLogQueue.put(buf);
        } catch (InterruptedException e) {
            logger.error("add back log data error, error info: {}", e);
            e.printStackTrace();
        }

    }

    /**
     * 具体消费流程
     */
    public void backLog() {
        try {
            CompositeByteBuf compositeBuf = getDataBufFromQueue();
            sendMessage(compositeBuf);
        } catch (InterruptedException e) {
            logger.error("get Buffer data from Queue error, error info: {}", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将Queue中数据转成要传输的ByteBuf
     *
     * @return
     * @throws InterruptedException
     */
    private CompositeByteBuf getDataBufFromQueue() throws InterruptedException {
        CompositeByteBuf compositeBuf = Unpooled.compositeBuffer();
        compositeBuf.writeInt(0);
        ByteBuf buf;
        int totalLength = 0;
        for (int i = 0; (buf = backLogQueue.poll(3, TimeUnit.SECONDS)) == null || i < 100; i++) {
            compositeBuf.addComponent(buf);
            totalLength += buf.readInt();
        }
        if (compositeBuf != null) {
            compositeBuf.setInt(0, totalLength);
        }

        return compositeBuf;
    }

    /**
     * 根据现有的连接，传递数据
     *
     * @param compositeBuf
     */
    private void sendMessage(CompositeByteBuf compositeBuf) {
        storeClient.getChannel().writeAndFlush(compositeBuf);
    }
}


