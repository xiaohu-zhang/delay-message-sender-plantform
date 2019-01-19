package com.cmcc.timer.mgr.init.messageLoader;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;

public interface LoadCallBack {
    void afterLoadLineInFile(FreezeModel model);

}
