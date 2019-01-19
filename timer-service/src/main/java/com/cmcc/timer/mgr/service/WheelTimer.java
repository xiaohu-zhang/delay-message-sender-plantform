package com.cmcc.timer.mgr.service;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;
import com.cmcc.timer.mgr.util.Timeout;

public interface WheelTimer {
    Timeout addTimeout(FreezeModel presentMoneyInputModel, int tryTimes);
}
