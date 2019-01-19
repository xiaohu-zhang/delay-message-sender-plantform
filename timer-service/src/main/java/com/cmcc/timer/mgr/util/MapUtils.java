package com.cmcc.timer.mgr.util;

import java.util.Map;

/** */
public class MapUtils {
	public static String getString(Map map, Object key) {
		if (map != null) {
			Object answer = map.get(key);
			if (answer != null) {
				return answer.toString();
			}
		}
		return "";
	}
	
	/** */
	public static Long getLong(Map map, Object key) {
	    if(map != null) {
	        return Long.parseLong(getString(map,key).trim());
	    }
		return 0l;
	}
	
	/** */
	public static Integer getInteger(Map map, Object key) {
		return Integer.parseInt(getString(map,key).trim());
	}
	
}
