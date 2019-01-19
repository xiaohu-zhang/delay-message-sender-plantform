package com.cmcc.timer.mgr.service.zk.impl;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import com.cmcc.timer.mgr.init.PreMasterMessageDeal;
import com.cmcc.timer.mgr.init.Starter;
import com.cmcc.timer.mgr.netty.SocketRelate;
import com.cmcc.timer.mgr.service.store.SyncByteStoreStrategy;
import com.cmcc.timer.mgr.service.store.backlog.BackLogStoreClient;
import com.cmcc.timer.mgr.service.store.backlog.BackLogStoreStrategy;
import com.cmcc.timer.mgr.service.zk.ZkClientExt;
import com.cmcc.timer.mgr.service.zk.ZkRegistryService;
import com.cmcc.timer.mgr.util.FileUtils;
import com.cmcc.timer.mgr.util.LocalHostUtil;
import com.cmcc.timer.mgr.util.SpringContextUtil;
import com.cmcc.timer.mgr.util.TimerUtils;

import io.netty.buffer.Unpooled;

/**
 * 服务向zookeeper上注册节点
 * 1. 节点绝对路径为：/timer/0/registryService/address-0x00000001
 * 其中，/timer表示根节点，名称固定；/0表示slot节点编号;/registryService表示服务节点，名称固定；
 * /address-0x00000001表示服务的临时节点，zk会根据注册的先后顺序，自动从小到大顺序编号，第一个节点表示主服务节点
 * 2. /timer节点数据存储所有slot节点的主服务节点信息，内容格式为：0/172.28.20.100:8080||1/172.28.20.200:8080
 * /0节点数据存储该slot下所有的服务节点信息，内容格式为：172.28.20.100:8080||172.28.20.101:8080
 * /address-0x00000001节点数据存储该节点的信息，内容格式为：172.28.20.100:8080
 * 3. 设置监听
 * /registryService节点设置子节点变化监听：将变化后的节点数据信息更新到/0中
 * /0节点设置节点数据变化监听：将变化后的数据信息截取主服务信息更新到/timer中
 * /timer节点设置子节点变化监听：将变化后的子节点数据信息更新到/timer中
 * <p>
 * 4. 启动BackLogStoreClient
 * 判定本机节点是否为slave节点，如果是，则启动对master节点的客户端
 */
public class ZkRegistryServiceImpl implements ZkRegistryService {
    private static Logger logger = LoggerFactory.getLogger(ZkRegistryServiceImpl.class);
    
    @Value("${server.port}")
    private int serverPort;

    @Value("${registry.servers}")
    private String zkServers;

    @Value("${registry.root.path}")
    private String registryPath;
    
    @Value("${data.path}")
    private String logPath;
    
    @Value("${monitorPath}")
    private String monitorPath;
    
    @Autowired
    private BackLogStoreClient storeClient;

    @Autowired
    private BackLogStoreStrategy storeStrategy;
    
    @Autowired
    private Starter starter;
    
    @Autowired
    private PreMasterMessageDeal slaveInitDeal;
    
    @Autowired
    private SpringContextUtil springContextUtil;

    private ZkClientExt zkClientExt;
    
    private int tcpPort = -1;

    public ZkRegistryServiceImpl() {
        logger.debug("初始化类");
    }

    public ZkRegistryServiceImpl(String zkServers) {
        zkClientExt = new ZkClientExt(zkServers);
    }

    /**
     * zookeeper节点注册
     *
     * @param slotCode 分组值
     */
    public void register(String slotCode,int totalSlot) throws UnknownHostException {
        //拼装本机节点信息
        String serviceAddress = String.format("%s:%d", LocalHostUtil.getLocalIp(), serverPort);
        //注册本机节点
        doRegister(slotCode, serviceAddress,totalSlot);
//        //根据本机为slave启动BackLogStoreClient
//        startBackLogClient(serviceAddress, slotCode);
    }

    /**
     * 实际节点注册逻辑
     *
     * @param slotCode
     * @param serviceAddress
     */
    public void doRegister(String slotCode, String serviceAddress,int totalSlot) {
        try {
            logger.debug("-ZkClient---------" + zkClientExt);
          //创建根节点：持久节点
            if (!zkClientExt.exists(registryPath)) {
                zkClientExt.create(registryPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                logger.debug("create registry node:{}", registryPath);
            }
            //为根节点创建Listener
            setRegistryNodeListener(registryPath,totalSlot);
//            registryNodeListenerLatch.await();

            //创建分组节点：持久节点
            String slotPath = registryPath + "/" + slotCode;
            if (!zkClientExt.exists(slotPath)) {
                zkClientExt.create(slotPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                logger.debug("create slot node:{}", slotPath);
            }
            //为slot节点创建Listener
            setSlotNodeListener(slotPath,totalSlot);
//            slotNodeListenerLatch.await();

            List<String> serviceLists = springContextUtil.getServiceLists();
            for (String sn : serviceLists) {
                String servicePath = slotPath + "/" + sn;
                //创建服务节点：持久节点
                if (!zkClientExt.exists(servicePath)) {
                    zkClientExt.create(servicePath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    logger.info("create service node :{}" + servicePath);
                }
                //为服务节点创建Listener
                setServiceNodeListener(servicePath);
//                serviceNodeListenerLatch.await();

                //创建地址节点：临时顺序节点
                String addressPath = servicePath + "/address-";
                //生成uuid，写入zookeeper 
                String nodeId = TimerUtils.getNodeId();
                //判断node.config文件是否写过
                Path nodeConfig = Paths.get(logPath ,"node.config");
                
                    if(nodeConfig.toFile().exists() && nodeConfig.toFile().length() != 40){
                        nodeConfig.toFile().delete();
                        FileUtils.createNewFile(nodeConfig.toString());
                        TimerUtils.storeByte(Paths.get(logPath ,"node.config"), Unpooled.wrappedBuffer(TimerUtils.getNodeIdBytes()));
                        TimerUtils.storeByte(Paths.get(logPath ,"node.config"), Unpooled.copyInt(Integer.valueOf(slotCode)));
                        //初始化的时候这里写1，当重启机器的时候，比对文件的version版本和zookeeper中存储的是否一致。不一致则全量同步数据。当有slot槽迁移的时候，需要增加version版本。
                        TimerUtils.storeByte(Paths.get(logPath ,"node.config"), Unpooled.copyInt(Integer.valueOf(1)));
                    }
                String tempData = "1^" + nodeId + "->" + serviceAddress;
                //判断该节点是否存在，已存在需要先删除此节点
                zkClientExt.delNodeIfExist(servicePath,nodeId);
                zkClientExt.delNodeIfExist(monitorPath,nodeId);
                //删除全部的socket
                SocketRelate.clear();
                TimerUtils.masterId = null;
                String addressNode = zkClientExt.createEphemeralSequential(addressPath, tempData);
                //这里还要监控session超时的情况，需要重建临时节点
//                setExpireListener(this,slotCode,totalSlot);
                setExpireListener(addressPath, tempData);
                setExpireListener(monitorPath + "/address-", TimerUtils.getNodeId());
                //注册到/monitor节点，并监控/timer/slot/service子节点的变化
                registerToMonitorNode(totalSlot);
                //记录slot节点值
                TimerUtils.slot = Integer.valueOf(slotCode);
                logger.debug("create node address:{}=>{}" + addressNode);
            }
        } catch (Exception e) {
            logger.error("create node failure", e);
            throw new RuntimeException("create node failure",e);
        }
    }
    
    
    
    
    @Override
    public void delNode(String nodeId) {
        if(null != nodeId){
            // TODO Auto-generated method stub
            List<String> serviceLists = springContextUtil.getServiceLists();
            for(String service : serviceLists){
                String tempPath = registryPath + "/" + TimerUtils.slot + "/" + service;
                zkClientExt.delNodeIfExist(tempPath, nodeId);
            }
            zkClientExt.delNodeIfExist(monitorPath, nodeId);
        }
    }

    private void registerToMonitorNode(int totalSlot) {
        createPathNeeded(monitorPath);
        watchByFirstEphemeralNode(monitorPath, totalSlot);
        zkClientExt.createEphemeralSequential(monitorPath + "/address-", TimerUtils.getNodeId());
    }
    
    private void createPathNeeded(String path){
        if(!zkClientExt.exists(path)){
            zkClientExt.create(path, null, CreateMode.PERSISTENT);
        }
    }
    
    private void watchByFirstEphemeralNode(String path, int totalSlot) {
        zkClientExt.subscribeChildChanges(path, (parentPath, currentChilds) -> {
            String monitorNode = currentChilds.stream().min((o1, o2) -> {
                return o1.compareTo(o2);
            }).orElse(null);
            // 判断是否是自身
            if (monitorNode != null && zkClientExt.getZkNodeData(parentPath + "/" + monitorNode).equals(TimerUtils.getNodeId())) {
                for (int i = 0; i < totalSlot; ++i) {
                    List<String> serviceLists = springContextUtil.getServiceLists();
                    for (String sn : serviceLists) {
                        String monitorPath = registryPath + "/" + i + "/" + sn;
                        //刚选举成功，需要查询一下，防止下面subscribe前，已经有slot节点为null
                        serviceMonitor(totalSlot,monitorPath,zkClientExt.getChildren(monitorPath));
                        zkClientExt.subscribeChildChanges(monitorPath, (parent, childrens) -> {
                            serviceMonitor(totalSlot, parent, childrens);
                        });

                    }
                }
            }
        });
    }

    private void serviceMonitor(int totalSlot, String parent, List<String> childrens) {
        if (childrens.size() == 0) {
            zkClientExt.writeData(getSlotPath(parent), null);
            setRegistryNodeData(registryPath, zkClientExt.getChildren(registryPath), totalSlot);
        }
    }
    

//    /**
//     * 查看本机是否为slave节点，是则启动本机的BackLogStoreClient
//     *
//     * @param localServer
//     */
//    public void startBackLogClient(String localServer, String slotCode) {
//        String[] slotDatas = zkClientExt.getZkNodeData(slotCode, "||");
//        if (slotDatas == null || slotDatas.length == 0) {
//            return;
//        }
//        String masterSever = slotDatas[0];
//        if (!masterSever.equals(localServer)) {
//            ThreadPool.getThreadPoolExecutor().execute(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        CountDownLatch latch = new CountDownLatch(1);
//                        storeClient.start(masterSever);
//                        latch.await();
//                        while (true) {
//                            storeStrategy.backLog();
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//        }
//    }

    /**
     * 为根节点设置子节点（slot节点）变更（节点创建、删除、数据变化）的Listener
     *
     * @param registryPath
     */
    private void setRegistryNodeListener(String registryPath,int totalSlot) {
        zkClientExt.subscribeChildChanges(registryPath, new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> currentChildren) throws Exception {
                setRegistryNodeData(registryPath, currentChildren,totalSlot);
            }
        });
//        registryNodeListenerLatch.countDown();
    }

    /**
     * 为slot节点设置数据变更的Listener
     * 主要用于服务节点下子节点变更后，更新了该slot节点的数据，然后将变更的数据更新到根节点中
     *
     * @param slotPath
     */
    private void setSlotNodeListener(String slotPath,int totalSlot) {
        zkClientExt.subscribeDataChanges(slotPath, new IZkDataListener() {
            @Override
            public void handleDataChange(String dataPath, Object data) throws Exception {
                updateRegistryNodeData(totalSlot,data);
            }

            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                updateRegistryNodeData(totalSlot,null);
            }
        });
//        slotNodeListenerLatch.countDown();
    }

    /**
     * 为服务节点设置子节点（临时节点）变更的Listener
     * 主要用于监控临时节点的主节点发生变化，如果最小节点发生变化，将最小节点的数据设置到slot节点中
     */
    private void setServiceNodeListener(String servicePath) {
        zkClientExt.subscribeChildChanges(servicePath, new IZkChildListener() {
            //当前子节点中如果最小的节点号和本机的nodeId相同，则说明当前节点为master。将当前的nodeId->ip:httpPort:TcpPort写入zookeeper中
            @Override
            public void handleChildChange(String parentPath, List<String> currentChildren) throws Exception {
                if(currentChildren == null || currentChildren.size() == 0){
                    //直接节点data域写入null
                    zkClientExt.writeData(getSlotPath(servicePath), null);
                    return;
                }
                
                //TODO 可以不用排序，直接list.stream().min()取最小值即可
                Collections.sort(currentChildren, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return Integer.parseInt(o1.substring(8)) - Integer.parseInt(o2.substring(8));
                    }
                });
                String temepData = zkClientExt.readData(servicePath + "/" + currentChildren.get(0));
                //解析是否为当前节点
                int masterIdStartIndex = temepData.indexOf("^")+1;
                String masterNodeId = temepData.substring(masterIdStartIndex, masterIdStartIndex + 32);
                StringBuilder sb = new StringBuilder();
                //此处只有主节点才会写zookeeper，因为如果主从都写数据，可能从写的数据会覆盖主写的数据，导致最终slot节点data域不存在tcpPort内容
                if(masterNodeId.equals(TimerUtils.getNodeId())){
                    currentIsMaster(servicePath, currentChildren, temepData, masterNodeId, sb);
                }
                //判断从节点的masterId是否匹配
                else{
                    if(TimerUtils.getMasterId() != null){
                        if(masterNodeId.equals(TimerUtils.getMasterId())){
                            //当前master未发生变化，无需启动slave,master 宕机再启动，一定会重新注册temp节点，此时原slave节点不挂，则必然启动后成功slave节点。只有主备同时挂掉，主备启动会load数据，无影响
                            return;
                        }
                }
                    //关闭现有的slave和master的网络连接，启动和新master的连接
                    SocketRelate.clear();
                    int i;
                    String masterNodeInfo = "";
                    for(i = 0;i < 3;++i){
                        if(StringUtils.countOccurrencesOf(temepData, ":") < 2){
                            //如果主刚注册，对应的新增节点的listenner尚未被触发，此时，尚未生成server的port。每隔5s重新获取节点信息
                            Thread.currentThread().sleep(5 * 1000);
                            temepData = zkClientExt.readData(servicePath + "/" + currentChildren.get(0));
                            continue;
                        }
                    masterNodeInfo = temepData.subSequence(temepData.indexOf(">")+1, temepData.indexOf(":"))+ ":" +
                            Integer.valueOf(temepData.substring(temepData.lastIndexOf(":")+1));
                    break;
                    }
                    if(i == 3){
                        SocketRelate.clear();
                        delNode(TimerUtils.getNodeId());
                        throw new RuntimeException("+++++++++++++++++++++++++++++++++++++++++++++++++master node data :" + masterNodeInfo + "don't have server port");
                    }
                    masterNodeId = temepData.substring(masterIdStartIndex, masterIdStartIndex + 32);
                    //多次重试后，本机既不为主，并且对应的主也发生了变化
                    if(!masterNodeId.equals(TimerUtils.getNodeId()) && !masterNodeId.equals(TimerUtils.getMasterId())){
                        //启动client
                        storeClient.start(masterNodeInfo,masterNodeId);
                        /**
                         * 1. 每个node存储masterId
                         * 2. 当启动时确认存储的masterId和当前是否一致，如果一致，master向slave询问postion时，给出positon。不一致，则返回-1.全量拷贝。
                         * 3. 当2全部完成，则才进行load操作。将数据load到slave。并且必须是同步load。load成功以后，才能正常的主备同步
                         */
                        
//                            starer.load0(LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
//                                    , Integer.MAX_VALUE, Integer.valueOf(LocalDateTime.now().format(TimeUtil.df)), null);
                        
                        //发送当前的本机nodeId，触发master发送多出的消息给slave
                        slaveInitDeal.sendNodeId();
                        //触发client和server之间数据交换
                        slaveInitDeal.dealNotSenderMessage(masterNodeId);
                    }
                }
                
                
            }

            private void currentIsMaster(String servicePath, List<String> currentChildren, String temepData, String masterNodeId,
                    StringBuilder sb) throws InterruptedException, ParseException {
                boolean masterRegister = false;
                String masterNode = currentChildren.get(0);
                String masterNodeData = "";
                if(StringUtils.countOccurrencesOf(temepData, ":") < 2){
                    //启动server端口，有slave的channel需要关闭
                    //关闭slave切换为主时的channel
                    SocketRelate.clear();
                    //必须在更改nodeType前load，防止本机先更改scanlog时间,必须在删除slaveIndex文件夹前，保证即是initLoad之后该节点断掉,重新成为slave节点的时候
                    //存在load文件，依然可以从load文件中load数据发送给主,注意此时尚未更改/timer节点的内容，因此尚不能提供对外服务
                    starter.initLoadByMaster();
                    //删除slaveIndex
                    TimerUtils.rMDeleteFile(Paths.get(logPath,"slaveindex"));
                        InetSocketAddress serverAddress =  starter.startServer();
                        //1. 备切换为主的情况---这种情况下只需要对end_time在scanlog和切换成功之间时间内的数据重新发送即可
                        //2. 只有一台机器，主宕机后重启的情况
                        //建立slaveIndex文件夹，表示为主(记得sendpre之后将slaveIndex删除掉,做好同步处理)
                        //建立slaveIndex dir 表名本机是主
                        synchronized (TimerUtils.slaveIndexDirSyn) {
                            TimerUtils.makeSlaveIndexDirtimestamp = System.currentTimeMillis();
                            FileUtils.createNewDir(Paths.get(logPath,"slaveindex").toString());
                        }
                        sb.append(temepData + ":" + serverAddress.getPort());
                        currentChildren.remove(0);
                        masterNodeData = sb.toString();
                        sb.append("|");
                        masterRegister = true;
                }
//                Set<String> slaves = SocketRelate.ctxByAdress.keySet();
//                Set<String> copyOfslaves = slaves.stream().collect(Collectors.toSet());
//                System.out.println("slaves.size():"+slaves.size());
//                System.out.println("currentChildren.size():"+currentChildren.size());
                currentChildren.forEach(cc -> {
                    String childData = zkClientExt.readData(servicePath + "/" + cc);
                    System.out.println("childData:" + childData);
                    sb.append(childData).append("|");
//                    String nodeId = childData.substring(2,34);
//                    copyOfslaves.remove(nodeId);
                });
//                for(String delNodeId : copyOfslaves){
//                    SocketRelate.removeChannel(SocketRelate.ctxByAdress.get(delNodeId));
//                }
                String serviceData = sb.toString().substring(0, sb.length() - 1);
                String slotData = zkClientExt.readData(getSlotPath(servicePath));
                //这里一定要在SocketRelate.removeChannel后操作，因为可能出现slave已经注册过ctxByAdress，然后slaves存在值，而在currentChildren本来只有一个值，就是当前master注册的值
                //但是currentChildren.remove(0);后内值为空，则SocketRelate.removeChannel会删除本不应该删除的在ctxByAdress中的slave的channel。因此这里必须先removeChannel
                //后注册本机的tcp节点，这样，在 SocketRelate.ctxByAdress还是为空，不会影响逻辑正确
                if(masterRegister){
                    TimerUtils.masterId = masterNodeId;
                    TimerUtils.currentNodetype = 0;
                    synchronized (TimerUtils.slaveWaitObject) {
                        TimerUtils.slaveWaitObject.notifyAll();
                    }
                    // 预置 SyncByteStoreStrategy.fileByDate
                    SyncByteStoreStrategy.getFileByDate().clear();
                    try(Stream<Path> p = Files.list(Paths.get(logPath,"redolog"))){
                        p.map(path->path.toFile().getName()).sorted().forEachOrdered(n->
                        SyncByteStoreStrategy.getFileByDate().put(n.substring(5, 15), Paths.get(logPath,"redolog",n).toString()));
                    }catch (Exception e) {
                        // TODO: handle exception
                        logger.error("when switch to master open redolog error",e);
                        throw new RuntimeException(e);
                    }
                  //在address节点data域增加tcpPort
                    zkClientExt.writeData(servicePath + "/" + masterNode,masterNodeData );
                }
                if (StringUtils.isEmpty(slotData) || !slotData.equals(serviceData)) {
                    zkClientExt.writeData(getSlotPath(servicePath), StringUtils.isEmpty(serviceData) ? null : serviceData);
                }
            }
        });
//        serviceNodeListenerLatch.countDown();
    }
    
    private void setExpireListener(ZkRegistryService zkRegistryService,String slotCode,int totalSlot){
        zkClientExt.subscribeStateChanges(new IZkStateListener(){

            @Override
            public void handleStateChanged(KeeperState state) throws Exception {
                // TODO Auto-generated method stub
                logger.error("current node KeeperState changed to " + state.toString());
            }

            @Override
            public void handleNewSession() throws Exception {
                // TODO Auto-generated method stub
                zkRegistryService.register(slotCode, totalSlot);
                logger.debug("zk session reconstruct in handleNewSession method ,call zkRegistryService.register with parm {},{}" ,slotCode, totalSlot);
            }

            @Override
            public void handleSessionEstablishmentError(Throwable error) throws Exception {
                // TODO Auto-generated method stub
                logger.error("error in connect to zookeeper ",error);
            }
            
        });
    }
    
    private void setExpireListener(final String addressPath, final String tempData){
        zkClientExt.subscribeStateChanges(new IZkStateListener(){

            @Override
            public void handleStateChanged(KeeperState state) throws Exception {
                // TODO Auto-generated method stub
                logger.error("current node KeeperState changed to " + state.toString());
            }

            @Override
            public void handleNewSession() throws Exception {
                // TODO Auto-generated method stub
                String addressNodePath = zkClientExt.createEphemeralSequential(addressPath, tempData);
                logger.debug("create node address:{}=>{}" + addressNodePath);
            }

            @Override
            public void handleSessionEstablishmentError(Throwable error) throws Exception {
                // TODO Auto-generated method stub
                logger.error("error in connect to zookeeper ",error);
            }
            
        });
    }

    /**
     * 根据服务路径获取slot的路径
     *
     * @param servicePath
     * @return
     */
    private String getSlotPath(String servicePath) {
        return servicePath.substring(0, servicePath.indexOf("/", registryPath.length() + 1));
    }

    /**
     * 根据子节点数据设置父节点数据，格式为：子节点1/子节点1数据||子节点2/子节点2数据
     * 主要用于根节点存储子节点（slot节点）的数据，即每个slot的主服务，数据为主服务的ip:port
     *
     * @param parentPath
     * @param currentChildren
     */
    private void setRegistryNodeData(String parentPath, List<String> currentChildren,int totalSlot) {
        if (currentChildren.size() == 0) {
            zkClientExt.writeData(parentPath, null);
            return;
        }
        Collections.sort(currentChildren, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Integer.parseInt(o1) - Integer.parseInt(o2);
            }
        });
        
        StringBuilder sb = new StringBuilder();
        for(int slotCode = 0;slotCode < currentChildren.size();++slotCode){
            String childData = null;
            try {
                childData = zkClientExt.readData(parentPath + "/" + currentChildren.get(slotCode));
            } catch (Exception e) {
                // ignore maybe the node haven't create completed yet.
                continue;
            }
            int interval = TimerUtils.TOTALSOLTNUMBER/totalSlot+1;
            int beginslot = interval * Integer.valueOf(slotCode);
            int endslot = beginslot + interval - 1 > TimerUtils.TOTALSOLTNUMBER-1 ? TimerUtils.TOTALSOLTNUMBER-1 : beginslot + interval - 1;
            String slotinterval = String.valueOf(beginslot) + "-" + String.valueOf(endslot);
            String[] serverDatas = new String[1];
            if(childData != null){
                serverDatas = childData.split("\\|");
            }else{
                serverDatas[0] = null;
            }
            sb.append(currentChildren.get(slotCode)).append("/")
            .append(slotinterval).append("/")
            .append(serverDatas[0]).append("||");
        }
        if(sb.length() != 0){
            zkClientExt.writeData(parentPath, sb.toString().substring(0, sb.length() - 2));
        }
    }

    /**
     * slot节点数据发生变化时，父节点（根节点）重新获取slot节点数据
     */
    private void updateRegistryNodeData(int totalSlot,Object data) {
        List<String> children = zkClientExt.getChildren(registryPath);
        setRegistryNodeData(registryPath, children,totalSlot);
        if(data == null){
            //关闭全部tcp连接
            SocketRelate.allChannels.close();
        }
    }

    public ZkClientExt getZkClientExt() {
        return zkClientExt;
    }
    
    public static void main(String...strings){
        
    }
}