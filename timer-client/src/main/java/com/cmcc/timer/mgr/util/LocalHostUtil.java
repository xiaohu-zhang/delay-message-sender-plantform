package com.cmcc.timer.mgr.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class LocalHostUtil {

    /**
     * 判断给定的机器信息中是否包含本机
     *
     * @param servers
     * @return
     * @throws UnknownHostException
     */
    public static boolean isEqualLocal(String servers,long tomcatPort) throws UnknownHostException {
        return servers.equals(getLocalIp()+":"+tomcatPort);
    }

    /**
     * 获取本机ip
     *
     * @return
     */
    public static String getLocalIp() throws UnknownHostException {
        String virtualIp = (String)TimerUtils.configMap.get("virtualIp");
        InetAddress addr = InetAddress.getLocalHost();
        if(!"".equals(virtualIp)){
            return virtualIp;
        }
        return addr.getHostAddress().toString();
    }
}
