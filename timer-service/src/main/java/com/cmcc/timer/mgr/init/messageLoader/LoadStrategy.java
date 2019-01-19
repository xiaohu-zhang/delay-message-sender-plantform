package com.cmcc.timer.mgr.init.messageLoader;

public interface LoadStrategy {
    public void load(String path,boolean ignoreError,long lastWriteTime,long maxModifyTime);
}
