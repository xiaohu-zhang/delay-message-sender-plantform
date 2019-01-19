package com.cmcc.timer.mgr.netty.serverHandle;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Stream;

import com.cmcc.timer.mgr.controller.ipresent.FreezeController;
import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.init.Starter;
import com.cmcc.timer.mgr.init.messageLoader.ByteLoadStrategy;
import com.cmcc.timer.mgr.netty.SocketRelate;
import com.cmcc.timer.mgr.netty.model.SlaveLogPostion;
import com.cmcc.timer.mgr.service.schedule.ScheduleService;
import com.cmcc.timer.mgr.util.FileUtils;
import com.cmcc.timer.mgr.util.TimeUtil;
import com.cmcc.timer.mgr.util.TimerUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private String logPath = "D:/data/Timer/logs"; 
    
    Starter starer = new Starter();
    
    private ByteLoadStrategy byteLoadStrategy;
    
    

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        SocketRelate.removeChannel(ctx);
        super.channelInactive(ctx);
    }


    /**index 名字yyyyMMddHH
     * index 文件格式 定长文件 最大长度为32 + 4 + 8 =44不足在ip最后补空格,int表示redo文件的最后一个后缀最大长度格式为:
     * uuid --- 32B
     * int --- file suffix
     * long --- file position
     * 
     * @throws IOException
     */
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf msgb = ((ByteBuf)msg);
        // 读取msg的第一个byte.根据byte确认不同的操作。
        Byte op = ((ByteBuf)msg).readByte();
        switch (op) {
        //记录的是原master节点，现在启动后为slave节点。slave index文件记录的是每个slave同步的postion位置。
        case 1:
            //master用来询问slave的postion
            //得到传入的masteruuid
            ByteBuf uuid = ((ByteBuf)msg).slice(1, 32);
            uuid.readerIndex(0);
            String muid = uuid.toString(CharsetUtil.UTF_8);
            //master发起对文件的postion的查询，注意，查询时间必定是当前时间以后的时间。以为之前的时间已经发送过了消息，补充数据没有意义
            //返回index文件的poston位置，如果没有，则发送对应文件名的postion ，master根据position和now的最大值发送数据
            //文件名yyyyMMddHH
            String fileName = ((ByteBuf)msg).toString(CharsetUtil.UTF_8);
            Path p = Paths.get(logPath ,"slaveindex",fileName);
            //index文件存在，则从index文件中获取,因为此机器是原master，index记录的是原slave接受到的文件位置
            SlaveLogPostion filePositionRelated =  getPosition(muid, p);
            ReferenceCountUtil.safeRelease(msg);
            if(filePositionRelated == null){
                //只能给出当前的文件位置 
                SlaveLogPostion temp = new SlaveLogPostion("",-1,-1);
                Path p1 = Paths.get(logPath ,"redolog");
                p1.forEach(f->{
                    if(f.toFile().getName().contains(fileName)){
                        String name =f.toFile().getName();
                        int s = Integer.valueOf(name.substring(name.lastIndexOf(".")+1));
                        if(temp.getSuffix() < s){
                            temp.setSuffix(s);
                            temp.setPosition(f.toFile().length());
                            temp.setFileName(name);
                        }
                    }
                });
                filePositionRelated = temp;
            }
            ByteBuf body = filePositionRelated.toByteBuf();
            ctx.write(Unpooled.wrappedBuffer(Unpooled.copyInt(4 + body.readableBytes()),body));
            break;
        case 2:
            // 发送方server主动询问对应的文件在slave中的postion，master可以将postion后的数据推送到slave端
            // 当收到2后,写入文件后，就可以删除index 了。
            //这里写的不对，应该是slave反序列化buf，直接顺序写入文件中 ，如果需要写入到内存，则直接写入到内存
            /**
             * 发送格式：
             * length --- fram length
             * byte ---- 2 
             * overwrite --- 1 true 0 false
             * fileNameLength --- filename的长度  byte
             * fileName ---- 写入的fileName
             * byte ----- 写入的msg
             * 问题：如何保证写入的msg是整的?这里不能直接给出任意一个endposition，因为这样传出的数据可能是半条数据，当此时master宕机，slave做master，半条的msg数据将无法载入。
             */
            boolean overwrite = (((ByteBuf)msg).readByte() == 1) ? true:false;
            int fileNameLength = ((ByteBuf)msg).readByte();
            String redoFileName = msgb.slice(msgb.readerIndex(), fileNameLength).toString(CharsetUtil.UTF_8);
            String yyyyMMddhh = redoFileName.substring(5,15);
            FileUtils.createNewFile(Paths.get(logPath,"redolog",redoFileName).toString());
            if(overwrite){
                try(FileWriter f = new FileWriter(Paths.get(logPath,"redolog",redoFileName).toString())){
                    f.write("");
                }
            }
//            redoFileName = FileUtils.stractMaxSuffixFileName(Paths.get(logPath, "redolog"),yyyyMMddhh);
            msgb.readerIndex(msgb.readerIndex() + fileNameLength);
            byte[] dsts = new byte[msgb.readableBytes()];
            msgb.readBytes(dsts);
            TimerUtils.storeByte(Paths.get(logPath ,"redolog",redoFileName), Unpooled.wrappedBuffer(dsts));
            /**
             * 根据条件判断是否入内存
             */
            Date redoLogDate = Date.from(LocalDateTime.parse(yyyyMMddhh, TimeUtil.df).atZone(ZoneId.systemDefault()).toInstant());
            FreezeModel fModel = new FreezeModel();
            fModel.setDeadTime(redoLogDate);
            if(FreezeController.setToTimeWheel(fModel)){
                byteLoadStrategy.loadFromBytes(dsts);
            }
            
            //slave 返回byte类型为2，表示处理完毕推送的数据
            /**
             * 返回数据格式
             * int -- total frame length 
             * byte --- 2
             * bytes -- 32 客户端32位标志符
             * int --  写入的字节数
             * bytes --- 写入的redo文件的文件名
             * 
             */
            msgb = Unpooled.buffer();
            msgb.clear();
            msgb.writeInt(0);
            msgb.writeByte(2);
            msgb.writeBytes(TimerUtils.uuid);
            msgb.writeInt(dsts.length);
            msgb.writeBytes(redoFileName.getBytes(CharsetUtil.UTF_8));
            msgb.setInt(0, msgb.writerIndex() - 4);
            ctx.writeAndFlush(msgb,ctx.voidPromise());
            break;
        case 3:
            //为心跳做准备,不用做心跳了，因为每1s就会同步一下scanlog
            break;
        case 4:
            Path slaveDir = Paths.get(logPath ,"slaveindex");
            try(Stream<Path> p2 = TimerUtils.getPathStream(slaveDir).get()){
                p2.forEach(f->{
                    f.toFile().deleteOnExit();
                });
            }
            break;
        case 5:
            //主向slave 同步scanlog的值
            ((ByteBuf)msg).readBytes(ScheduleService.getScanChannel(), 0, 8);
            break;
        case 6:
            ctx.channel().attr(SocketRelate.slaveToMasterKey).get().countDown();
            break;
        default:
            break;
        }
        super.channelRead(ctx, msg);
    }
    
        
    public static SlaveLogPostion getPosition(String muid, Path p) throws IOException {
        if(p.toFile().exists()){
            ByteBuffer ipUuid = ByteBuffer.allocate(32);
            try(FileChannel fc = FileChannel.open(p, StandardOpenOption.READ)){
                int readBeginPostion = 0;fc.position();
                for(;readBeginPostion < fc.size();readBeginPostion += 44){
                    fc.read(ipUuid);
                    ipUuid.flip();
                    String logmUid = new String(ipUuid.array(),CharsetUtil.UTF_8);
                    ipUuid.clear();
                    if(muid.trim().equals(logmUid)){
                        ByteBuffer filePosition = ByteBuffer.allocate(8);
                        ByteBuffer suffixb = ByteBuffer.allocate(4);
                        fc.read(suffixb);
                        fc.read(filePosition);suffixb.flip();filePosition.flip();
                        int suffix = suffixb.getInt();
                        long postion = filePosition.getLong();
                        filePosition.clear();
                        suffixb.clear();
                        String logFileName = "redo." + p.toFile().getName() + ".log." + suffix;
                        return new SlaveLogPostion(logFileName, postion,suffix);
                    }
                }
               return new SlaveLogPostion();
            }
        }
        return new SlaveLogPostion();
    }
    
    
    
    public ClientInboundHandler(String logPath,ByteLoadStrategy byteLoadStrategy) {
        super();
        this.logPath = logPath;
        starer.setLogPath(logPath);
        this.byteLoadStrategy = byteLoadStrategy;
    }

    public static void main(String...strings) throws IOException{
//        ClientInboundHandler h = new ClientInboundHandler("D:/data/Timer/logs");
//        System.out.println(h.stractMaxSuffixFileName("2008050215"));
    }
    
}
