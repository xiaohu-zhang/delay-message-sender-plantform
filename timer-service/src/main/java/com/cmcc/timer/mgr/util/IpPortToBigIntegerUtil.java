package com.cmcc.timer.mgr.util;

import java.math.BigInteger;

public class IpPortToBigIntegerUtil {
    /** 
     * ip地址转成bigInteger型数字 
     * @param strIp 
     * @return 
     */  
    public static String ipPortToNumString(String strIpPort) {  
        int colonIndex = strIpPort.indexOf(":");
        if(colonIndex==-1){
            throw new RuntimeException("strIpPort must contains : and port");
        }
        String port = strIpPort.substring(colonIndex+1);
        String[]ip = strIpPort.substring(0, colonIndex).split("\\.");  
        BigInteger num = BigInteger.valueOf(Integer.parseInt(ip[0]));
        for(int i = 1;i < 4;i++){
            num = num.shiftLeft(8).add(BigInteger.valueOf(Integer.parseInt(ip[i]))); 
        }
        num = num.shiftLeft(16).add(new BigInteger(port));
        return num.toString();  
    }  
  
    /** 
     * @param longIp 
     * @return 
     */  
    public static String numStringToIP(String IpNum) { 
        BigInteger num = new BigInteger(IpNum);
        StringBuffer sb = new StringBuffer("");  
        for(int i = 40;i > 8;i -= 8){
            BigInteger tempXor = BigInteger.valueOf(0xff).shiftLeft(i);
            sb.append(String.valueOf((num.and(tempXor).shiftRight(i))));  
            sb.append(".");  
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append(":");
        sb.append(num.and(BigInteger.valueOf(0xffff)) );  
        return sb.toString();  
    }  
  
    public static void main(String[] args) {  
        System.out.println(ipPortToNumString("219.239.110.138:8080"));  
        System.out.println(numStringToIP("241821398212496"));  
    } 
}
