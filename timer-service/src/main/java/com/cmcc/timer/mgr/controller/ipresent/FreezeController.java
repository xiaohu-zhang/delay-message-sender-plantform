package com.cmcc.timer.mgr.controller.ipresent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cmcc.timer.mgr.ExceptionHandler.MgrExceptionFactory;
import com.cmcc.timer.mgr.annotation.Cmodel;
import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.service.PreDealService;
import com.cmcc.timer.mgr.service.RedisClusterService;
import com.cmcc.timer.mgr.service.WheelTimer;
import com.cmcc.timer.mgr.service.store.StoreStrategy;
import com.cmcc.timer.mgr.util.HashedwheeltimerWapper;
import com.cmcc.timer.mgr.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/freeze")
public class FreezeController {
    
    @Autowired
    private WheelTimer wheelTimer;
    
    @Autowired
    @Qualifier("addSyncByteStoreStrategy")
    private StoreStrategy addStoreStrategy;
    
    @Autowired
    @Qualifier("cancelSyncByteStoreStrategy")
    private StoreStrategy cancelStoreStrategy;
    
    @Autowired
    private RedisClusterService clusterService;
    
    @Autowired
    private MgrExceptionFactory expFactory;
    
    @Autowired
    private PreDealService preDealService;

    public static ObjectMapper mapper = new ObjectMapper();


    @RequestMapping(value="/test")
    public String DeFreezeTest(){
        System.out.println("getRequest");
        return "";
    }
    
    @RequestMapping(name="freezeservice",value = "/autoDeFreeze")
    public String autoDeFreeze(@Cmodel @Validated FreezeModel presentMoneyInputModel) throws Exception {
        preDealService.preDeal(presentMoneyInputModel);
        String setToLocal = clusterService.setMsgToLocal(presentMoneyInputModel);
        //当前机器可以接收此请求
        if("OK".equals(setToLocal)) {
        	addStoreStrategy.store(presentMoneyInputModel,"/redolog/redo.");
            //在1h内的过期消息，直接入内存,当前时间在[50-59]分，deadtime在下个hour的，也需要入内存。因为可能出现load的后台任务已经load过了对应的文件，则这条消息就丢失了
        	//此处一定是先store，后insert，这样判定的时间以落盘时间为基准确认是否入内存.如果store是缓存的，则这里setToTimeWheel需要50分-缓冲刷盘时间为判断点，而不能以50分为判断点
            if( setToTimeWheel(presentMoneyInputModel)){
                wheelTimer.addTimeout(presentMoneyInputModel, 0);
            }
            
        } else {
            throw expFactory.createException("100000001");
        }
        return "success";
    }

    public static boolean setToTimeWheel(FreezeModel presentMoneyInputModel){
        LocalDateTime now = LocalDateTime.now();
        return (presentMoneyInputModel.getDeadTime().compareTo(Date.from(now.plusHours(1).withMinute(0).withSecond(0).withNano(0).atZone(ZoneId.systemDefault()).toInstant())
                ) <= 0 ) || (now.get(ChronoField.MINUTE_OF_HOUR) >= 50 && presentMoneyInputModel.getDeadTime().compareTo(Date.from(now.plusHours(2).withMinute(0).withSecond(0).withNano(0).atZone(ZoneId.systemDefault()).toInstant())
                        ) <= 0 );
    }


    @RequestMapping(value = "/cancleDeFreeze")
    /**
     * 注意返回true并非表示cancel一定成功，因为基于底层实现，如果该延时消息已经在内存中，是可以判断的。但是当尚未在内存中，只能写入到redo file文件中，
     * 基于时序问题取消失败只有等到执行到load操作后才知道。这里true仅表示接受处理成功
     * @param presentMoneyInputModel
     * @return
     * @throws Exception
     */
    public boolean cancleDeFreeze(@Cmodel FreezeModel presentMoneyInputModel) throws Exception {
        Timeout t = HashedwheeltimerWapper.getInstance().getTimeoutTaskByPayType().get(presentMoneyInputModel);
        FreezeModel cancleModel = new FreezeModel();
        cancleModel.setFreezeSn(presentMoneyInputModel.getFreezeSn());
        int memoryMsgCancelFlag = -1;
        if(t == null){
            //从redis中查询该msg
            LocalDateTime deadTime = clusterService.get(presentMoneyInputModel);
            cancleModel.setDeadTime(Date.from(deadTime.atZone(ZoneId.systemDefault()).toInstant()));
        }else{
            memoryMsgCancelFlag = HashedwheeltimerWapper.getInstance().cancel(presentMoneyInputModel);
           if(memoryMsgCancelFlag < 3){
               return true;
           }
            cancleModel.setDeadTime(((FreezeModel)t.getExt()).getDeadTime());
        }
        cancleModel.setCreateTime(presentMoneyInputModel.getCreateTime());
        cancleModel.setTopic(presentMoneyInputModel.getTopic());
        cancelStoreStrategy.store(cancleModel,"/redolog/redo.");
        if(memoryMsgCancelFlag >= 3){
            clusterService.delMsgToLocal(cancleModel);//此处后续由异步发送线程处理删除任务
        }
        return true;
    }

    private Logger logger = LoggerFactory.getLogger(FreezeController.class);
    

}
