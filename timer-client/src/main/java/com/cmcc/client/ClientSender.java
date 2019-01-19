package com.cmcc.client;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.I0Itec.zkclient.IZkDataListener;
import org.springframework.util.StringUtils;

import com.cmcc.client.model.CancelMsgModel;
import com.cmcc.client.model.SendMsgModel;
import com.cmcc.timer.mgr.service.zk.ZkClientExt;
import com.cmcc.timer.mgr.util.FailedHandler;
import com.cmcc.timer.mgr.util.HeaderUtil;
import com.cmcc.timer.mgr.util.HttpUtil;
import com.cmcc.timer.mgr.util.TimeUtil;
import com.cmcc.timer.mgr.util.ZkCountSlotUtil;

public class ClientSender {
        
    private ZkClientExt zkClient;
    
    private String watchPath =  "/timer";
    
    private volatile List<String> servers = new ArrayList<>();
    
    private String autoDeFreezeUrl = "/Timer/freeze/autoDeFreeze";
    
    private String cancleDeFreezeUrl = "/Timer/freeze/cancleDeFreeze";
    
    public void asySend(SendMsgModel sendModel){
        asySend(sendModel,null);
    }
    
    public void asyCacle(CancelMsgModel cancelModel){
        asyCacle(cancelModel,null);
    }
    
    public void asySend(SendMsgModel sendModel,FailedHandler failedHandler){
        String deadTime = LocalDateTime.ofInstant(sendModel.getDeadTime().toInstant(), ZoneId.systemDefault()).format(TimeUtil.dfh);
        String postString = "{\"ROOT\": {\"BODY\": {\"BUSI_INFO\": {\"freezeSn\": \""
                + sendModel.getFreezeSn() + "\",\"deadTime\":\"" + deadTime
                + "\",\"gourpId\":\"0\",\"topic\":\"0\"" + ",\"createTime\":" + sendModel.getCreateTime() + "}}}}";
        int slotNodeIndex = ZkCountSlotUtil.getCRC16(sendModel.getFreezeSn(), servers.size());
        if(servers.get(slotNodeIndex) == null){
            throw new RuntimeException("the slot " + slotNodeIndex + " server is null");
        }
                HttpUtil.asynsend("http://" + servers.get(slotNodeIndex) + autoDeFreezeUrl, HeaderUtil.getJsonMap(), postString
                , (jsonStr) -> {},failedHandler); 
    }
    
    public void asyCacle(CancelMsgModel cancelModel,FailedHandler failedHandler){
        String postString = "{\"ROOT\": {\"BODY\": {\"BUSI_INFO\": {\"freezeSn\": \""
                + cancelModel.getFreezeSn() 
                + "\",\"gourpId\":\"0\",\"topic\":\"0\"" + ",\"createTime\":" + cancelModel.getCreateTime() + "}}}}";
        int slotNodeIndex = ZkCountSlotUtil.getCRC16(cancelModel.getFreezeSn(), servers.size());
        if(servers.get(slotNodeIndex) == null){
            throw new RuntimeException("the slot " + slotNodeIndex + " server is null");
        }
                HttpUtil.asynsend("http://" + servers.get(slotNodeIndex) + cancleDeFreezeUrl, HeaderUtil.getJsonMap(), postString
                , (jsonStr) -> {},failedHandler); 
    }

    
    public ClientSender(String zkServers) {
    	this(zkServers,"/timer");
    }

	public ClientSender(String zkServers,String watchPath) {
        zkClient = new ZkClientExt(zkServers);
        this.watchPath = watchPath;
        watchAndReturnServers(watchPath);
        init();
    }
    
    private void init(){
        String rootData = (String)(zkClient.readData(watchPath,true));
        if(rootData != null){
            rootDataChange(rootData);
        }else{
            throw new RuntimeException("the /timer data can't be null");
        }
    }
    
    private void watchAndReturnServers(String watchPath){
        zkClient.subscribeDataChanges(watchPath, new IZkDataListener(){

            @Override
            //0/0-6384/1^d0ccb5e0d7144ad6a40f7c31824e6b97->127.0.0.1:7888:59191||1/6385-16384/1^d0ccb5e0d7144ad6a40f7c31824e6b92->127.0.0.1:7889:59191
            public void handleDataChange(String dataPath, Object data) throws Exception {
                // TODO Auto-generated method stub
                rootDataChange(data);
                
            }

            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                // TODO Auto-generated method stub
                throw new RuntimeException("the /timer data can't be deleted");
            }
            
        });
    }
    

    private void rootDataChange(Object data) {
        String[] serverInfos = ((String)data).split("\\|\\|");
        List<Integer> maxNodes = new ArrayList<>(4);
        List<String> serverstemp = new ArrayList<>(4);
        for(String info:serverInfos){
            int indexOfFirstLine = info.indexOf("-");
            maxNodes.add(Integer.valueOf(info.substring(indexOfFirstLine+1, info.indexOf("/", indexOfFirstLine+1))));
            if(StringUtils.countOccurrencesOf(info, ":") < 2){
                serverstemp.add(null);
            }else{
                serverstemp.add(info.substring(info.indexOf(">")+1, info.lastIndexOf(":")));
            }
        }
            servers = serverstemp;
    }
    
    

    public static void main(String...strings){
        ClientSender sender = new ClientSender("172.28.20.215:2181","/timerdev");
        SendMsgModel sendModel = new SendMsgModel();
        sendModel.setDeadTime(new Date());
        sendModel.setFreezeSn("test1");
        sendModel.setGourpId("0");
        sendModel.setTopic("0");
        sender.asySend(sendModel);
        for(;;){
            
        }
    }
    
}
