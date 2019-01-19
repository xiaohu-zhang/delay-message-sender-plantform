package com.cmcc.timer.mgr.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;

@Service
public class PreDealServiceImpl implements PreDealService {

    @Override
    public void preDeal(FreezeModel freezeModel) {
        adjustDeadTime(freezeModel);
        setDelayTime(freezeModel);
    }
    
    private void adjustDeadTime(FreezeModel freezeModel){
        Date now = new Date();
        if(now.compareTo(freezeModel.getDeadTime()) > 0){
            freezeModel.setDeadTime(now);
        }
    }
    
    private void setDelayTime(FreezeModel presentMoneyInputModel) {
        presentMoneyInputModel.setDelayTime(
                ChronoUnit.SECONDS.between(LocalDateTime.now(),presentMoneyInputModel.getDeadTime().toInstant().
                        atZone(ZoneId.systemDefault()).toLocalDateTime()));
    }
}
