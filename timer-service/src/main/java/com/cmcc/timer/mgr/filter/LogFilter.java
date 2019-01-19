package com.cmcc.timer.mgr.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.cmcc.timer.mgr.controller.model.ThreadLocalModel;
import com.cmcc.timer.mgr.httpStream.ByteArrayServletOutputStream;
import com.cmcc.timer.mgr.httpStream.MgrResponseWrapper;

/**
 * 记录传入/出参数，
 * 调用inputstream记录传入参数后，use attributeSet方法过滤掉无用的字符后, 调用setAttribute方法以cModel为参数传入web内部
 * 对doFilter后的response，加入一些返回内容后,调用outputstream记录传出参数
 * @author silver
 *
 */
@Component
public class LogFilter implements Filter {

    private ThreadLocal<ThreadLocalModel> threadLocal = ThreadLocalModel.threadLocal;
   
    
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain arg2)
            throws IOException, ServletException {
        try {
            if (!(servletResponse instanceof HttpServletResponse)) {
                throw new RuntimeException("不是http请求");
            }
            HttpServletRequest hr = (HttpServletRequest) servletRequest;
            HttpServletResponse hs = new MgrResponseWrapper((HttpServletResponse) servletResponse);
            // TODO Auto-generated method stub
            String traceLog = hr.getParameter("tracelog");
            String localIp =InetAddress.getLocalHost().getHostAddress();
            threadLocal.set(new ThreadLocalModel(getIpAddr(hr), localIp==null?"127.0.0.1":localIp,traceLog,
            		hr.getRequestURL()+"?" +hr.getQueryString(),new Date(),String.valueOf(hr.getLocalPort())));
            String inputString = inuputLog(hr, hs);
            attributeSet(hr,inputString);
            arg2.doFilter(hr, hs);
            outputLog(hr,hs);
        }finally {
            ThreadLocalModel.threadLocal.remove();
        }
    }
    
    
    private void outputLog(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException{
        ServletOutputStream o = servletResponse.getOutputStream();
        ByteArrayServletOutputStream bos = (ByteArrayServletOutputStream)o;
        String ouString = new String(bos.toByteArray(),"UTF-8");
        if(StringUtils.isEmpty(ouString)){
            throw new RuntimeException("the response body is empty " + "traceId:" + ThreadLocalModel.getThreadLocal().getTreceId());
        }
        if(!ouString.contains("RETURN_CODE" )){
            ouString =  "{\"RETURN_CODE\": \"0\","
                    + "\"RUN_IP\": \""+ threadLocal.get().getLocalIp() + "\","
                    + "\"TRACEID\":\""+  threadLocal.get().getTreceId()+"\","
                    + "\"OUT_DATA\":" + ouString
                            + "}" ;
        }
        logger.info("response url:" + threadLocal.get().getUrl() + " remoteIp:" + threadLocal.get().getRemoteIp() + " return:" +ouString);
//        servletResponse.addHeader("Content-Length", String.valueOf(ouString.getBytes("UTF-8").length));
        ((MgrResponseWrapper)servletResponse).getResponse().getOutputStream().write(ouString.getBytes("UTF-8"));
    }
    


    private String inuputLog(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

        
        StringBuilder sb = new StringBuilder();
        String str;
        try (BufferedReader br = servletRequest.getReader()) {
            while ((str = br.readLine()) != null){
            	sb.append(str);
            }
        }
        logger.info("request url:" + threadLocal.get().getUrl() + " remoteIp:" + threadLocal.get().getRemoteIp() + " Body:" + sb.toString());
        return sb.toString();
    }
    
    /**
     * convert the input http body like: 
     * { "ROOT": 
     *      { "BODY": 
     *          { "BUSI_INFO": {
     *              "companyId": "02000006", 
     *              "migusum":"100",
     *              "effectiveTimeBegin":"201512011", 
     *              "effectiveTimeEnd":"201612021",
     *              "activityCode":"001" } } } } 
     *    to 
     *  { 
     *      "companyId": "02000006",
     *      "migusum":"100", 
     *      "effectiveTimeBegin":"201512011",
     *      "effectiveTimeEnd":"201612021", 
     *      "activityCode":"001" 
     *  }
     * 
     * @param servletRequest
     * @param inputString
     */
    private void attributeSet(HttpServletRequest servletRequest,String inputString){
        String convertedString = inputString;
        int indexBUSI = inputString.indexOf("BUSI_INFO");
        if(indexBUSI !=  -1){
            int startIndex = inputString.indexOf("{", indexBUSI);
            int endIndex = inputString.indexOf("}",inputString.lastIndexOf("\"",inputString.length()));
            convertedString = inputString.substring(startIndex, endIndex+1);
        }
        servletRequest.setAttribute("cModel", convertedString);
        servletRequest.setAttribute("traceId", threadLocal.get().getTreceId());
    }

    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub

    }

    private String getIpAddr(HttpServletRequest request) {
        String ipAddress = null;
        // ipAddress = this.getRequest().getRemoteAddr();
        ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if ("127.0.0.1".equals(ipAddress)) {
                // 根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                    ipAddress = inet.getHostAddress();
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    logger.error("getIpError", e);
                }
            }

        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
                                                            // = 15
            if (ipAddress.indexOf(",") > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }

    private Logger logger = LoggerFactory.getLogger(LogFilter.class);

    public void destroy() {
        // TODO Auto-generated method stub

    }

}
