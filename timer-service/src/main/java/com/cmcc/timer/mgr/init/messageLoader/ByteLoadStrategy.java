package com.cmcc.timer.mgr.init.messageLoader;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.controller.model.ipresent.OpEnum;
import com.cmcc.timer.mgr.exception.CreateTimeOrderException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import sun.nio.ch.FileChannelImpl;

/**
 * 不用考虑机器突然断电，最后一行数据损耗的情况，这种情况也没法有效的处理
 * 如果是kill的方式，操作系统可以保证，写入一定是原子的，都没写入进去或者都写入成功 此处是单线程读取
 * 
 * @author silver
 *
 */
@Service
public class ByteLoadStrategy implements LoadStrategy {

    ByteBuf buf = Unpooled.wrappedBuffer(ByteBuffer.allocate(8));
    @Autowired
    @Qualifier("addMessageLoader")
    private MessageLoader addMessageLoader;
    
    @Autowired
    @Qualifier("cancelMessageLoader")
    private MessageLoader cancelMessageLoader;

    public void load(String path, boolean ignoreError,long lastWriteTime,long maxModifyTime) {
        try (FileChannel fChannel = FileChannel.open(Paths.get(path), StandardOpenOption.READ);) {
            int fileEndPostion = new Long(fChannel.size()).intValue();
            loadInternal(fChannel, fileEndPostion, lastWriteTime,maxModifyTime);
        } catch (Exception e) {
            // 如果前面有问题，可能是整个文件的格式都不正常。则先忽略当前的整个文件
            if (!ignoreError) {
                throw new RuntimeException(e);
            }
        }
    }
    
//    public static ByteBuf stractAddOp(FileChannel fChannel, long index) throws IOException{
//        int byteLength = (int)(fChannel.size() - index);
//        if(byteLength > 0) {
//            ByteBuf bf = Unpooled.buffer(byteLength);
//            ByteBuf bfreturn = Unpooled.buffer(byteLength);
//            fChannel.read(bf.nioBuffer(), index);
//            //解析bf
//            int position = 0;
//            while(bf.readerIndex() < byteLength){
//                bf.markReaderIndex();
//                int totallength = bf.readInt();
//                Byte op = bf.readByte();
//                if(op.intValue() == OpEnum.Add.getValue()){
//                    bf.resetReaderIndex();
////                    bfreturn.writeBytes(bf.slice(index, length));
//                    
//                }
//            }
//        }
//    }
    
    public  void loadFromBytes(byte[] bytes){
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        int position = 0;
        while(position < bytes.length){
            buf.readerIndex(position);
            int lineSize = buf.readInt();
            int opCode = buf.readByte();
            if(opCode == OpEnum.Add.getValue()){
                try {
                    addMessageLoader.load(buf, lineSize, 0,Long.MAX_VALUE);
                } catch (CreateTimeOrderException e) {
                    // TODO Auto-generated catch block
                }
            }else if(opCode == OpEnum.Cancel.getValue()){
                cancelMessageLoader.load(buf, lineSize, 0,Long.MAX_VALUE);
            }
            position += lineSize;
        }
    }
    

    private void loadInternal(FileChannel fChannel, int fileEndPostion, long lastWriteTime,long maxModifyTime) throws IOException {
        MappedByteBuffer buffer = fChannel.map(MapMode.READ_ONLY, 0, fileEndPostion);
        try {
            ByteBuf buf = Unpooled.wrappedBuffer(buffer);
            int position = 0;
            int fileSize = fileEndPostion;
            while(position < fileSize){
                buf.readerIndex(position);
                int lineSize = buf.readInt();
                int opCode = buf.readByte();
                if(opCode == OpEnum.Add.getValue()){
                    try {
                        addMessageLoader.load(buf, lineSize, lastWriteTime,maxModifyTime);
                    } catch (CreateTimeOrderException e) {
                        // TODO Auto-generated catch block
                    }
                }else if(opCode == OpEnum.Cancel.getValue()){
                    cancelMessageLoader.load(buf, lineSize, lastWriteTime,maxModifyTime);
                }
                position += lineSize;
                
            }
        } finally{
         // 加上这几行代码,手动unmap,因为需要删除文件，如果不手动释放mmap，则可能会导致删除文件失败  
            Method m;
            try {
                m = FileChannelImpl.class.getDeclaredMethod("unmap",  
                        MappedByteBuffer.class);
                m.setAccessible(true);  
                m.invoke(FileChannelImpl.class, buffer);  
            } catch (Exception e) {
                // TODO Auto-generated catch block
                logger.error("umap buffer error.. ",e);
            }  
        }
    }

    public static void main(String... strings) throws IOException {
        ByteBuf bf = Unpooled.buffer();
        bf.writeInt(100);
        System.out.println(bf.readerIndex());
        System.out.println(bf.writerIndex());
        ByteBuf b = bf.slice(1, 1);
        System.out.println(b.readerIndex());
        System.err.println(b.writerIndex());
        b.writerIndex(0);
        System.out.println(b.writerIndex());
        System.out.println(bf.writerIndex());
        
    }

    private static volatile int i = 0;

    private static void MM() throws IOException {
        Paths.get("C:/home/migupay/logs/redolog").toFile().mkdirs();
        // FileChannel f =
        // FileChannel.open(Paths.get("C:/home/migupay/logs/redolog/redo.20180106.log"),
        // StandardOpenOption.READ,StandardOpenOption.WRITE);
        // MappedByteBuffer bb = f.map(MapMode.READ_WRITE, 0, 50000);
        //// ByteBuf bb = Unpooled.wrappedBuffer(b);
        //// System.out.println(bb.capacity());
        //// System.out.println(bb.getByte(0));
        //// System.out.println(bb.getByte(0));
        //// ByteBuffer bb = ByteBuffer.allocate(1);
        // ByteBuf b = Unpooled.wrappedBuffer(bb);
        //// ByteBuf b1 = Unpooled.buffer(1);
        //// ByteBuffer b = b1.nioBuffer();
        //// bs.capacity(1);
        //
        //// f.position(0);
        //// f.position();
        //// b.clear();
        //// b.writeByte(10);
        //// f.write(b.nioBuffer());
        // ByteBufInputStream r = new ByteBufInputStream(b,true);
        // r.close();
        //
        // while(true){
        // if(i == 0){
        // ByteBuffer bs = ByteBuffer.allocate(1);
        // ByteBuf bf = Unpooled.wrappedBuffer(bb);
        // bf.writerIndex(0);
        // bf.writeByte(100);
        // f.write(bf.nioBuffer(),f.size());
        // i++;
        // }else{
        //
        // b.writerIndex((int)f.size()-1);
        // b.writeByte(i++);
        // }

        // f.position(f.size()-1);
        // f.position();
        // f.read(b.nioBuffer());
        //// System.out.println(bb.readableBytes());
        ////// b.writerIndex(1);
        //// System.out.println(b.readByte());
        // b.readerIndex(0);
        // f.read(b.nioBuffer());
        // System.out.println(b.readByte());
        // b.clear();
        // f.position(f.size()-1);
        // f.position();
        // b.writeByte(i++);
        // f.write(b.nioBuffer());f.force(true);
        // }
    }
    
    private Logger logger = LoggerFactory.getLogger(ByteLoadStrategy.class);
}
