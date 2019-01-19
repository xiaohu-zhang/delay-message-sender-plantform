package com.cmcc.timer.mgr.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
/** */
public class HeaderUtil {
	/** */
	public static Map<String, String> getJsonMap(){
		Map<String, String> headerMap = new HashMap<String,String>();
		headerMap.put("Content-Type", "application/json");
		headerMap.put("charset", "UTF-8");
		headerMap.put("Accept", "application/json");
    	return headerMap;
	}
	
	public static Map<String, String> getTextMap(){
		Map<String, String> headerMap = new HashMap<String,String>();
		headerMap.put("Content-Type", "application/json");
		headerMap.put("charset", "UTF-8");
		headerMap.put("Accept", "text/plain;charset=UTF-8");
    	return headerMap;
	}
	
	/**
	 * 产生traceLog，产生规则：hhmmss+n位随机数字
	 */
	public static String createTraceId(int n){
		LocalDateTime date = LocalDateTime.now();
		DateTimeFormatter format = 	DateTimeFormatter.ofPattern("HHmmss");
		String dateStr = format.format(date);
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for(int i=0;i < n;i++){
			sb.append(random.nextInt(10));
		}
		return dateStr+sb;
	}
	/**
	 * 从远程调用接口返回json字符串中取出key值对应的数据
	 * @return
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public static String getStringFromReturnJson(String jsonStr) throws JsonParseException, JsonMappingException, IOException{
		String result = "";
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> map = mapper.readValue(jsonStr, Map.class);
		 Object root = map.get("ROOT");
		 if(root!=null&&(root instanceof Map )){
			 Map rootMap = (Map) root;
			 Object body = rootMap.get("BODY");
			 if(body!=null&&(body instanceof Map )){
				 Map bodyMap = (Map) body;
				 Object data = bodyMap.get("OUT_DATA");
				 if(data!=null&&(data instanceof Boolean)){
					 result = String.valueOf(data);
				 }
			 }
		 }
		 return result;
	}
	
	/**
	 * @throws JsonProcessingException  */
	public static String map2JsonString(Map<String,Object> inMap) throws JsonProcessingException{
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> infoMap = new HashMap<String,Object>();
		infoMap.put("BUSI_INFO", inMap);
		Map<String,Object> bodyMap = new HashMap<String,Object>();
		bodyMap.put("BODY", infoMap);
		Map<String,Object> rootMap = new HashMap<String,Object>();
		rootMap.put("ROOT", bodyMap);
		return mapper.writeValueAsString(rootMap);
	}
}
