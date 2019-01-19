package com.cmcc.timer.mgr.service.store;

import com.cmcc.timer.mgr.controller.model.ipresent.FreezeModel;

public interface StoreStrategy {
    
    void store(FreezeModel model,String prefixpath);
}
