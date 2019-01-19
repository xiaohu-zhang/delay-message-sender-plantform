package com.cmcc.timer.mgr.util;

import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.UUID;

import com.cmcc.timer.mgr.controller.model.ThreadLocalModel;


/**
 * @Type CUUID.java
 * @Desc 沿用原mgr工具类
 * @author tab_xu@qq.com
 * @date 2017年3月13日 下午4:36:00
 * @version 1.0
 */
public class CUUID {
	/**
	 * @Description description
	 */
	public static String createUuid() {

		String s = UUID.randomUUID().toString();
		return s.substring(0, 8) + s.substring(9, 13) + s.substring(14, 18)
				+ s.substring(19, 23) + s.substring(24);
	}

	/**
	 * @Description description
	 */
	public static String[] createUuid(int num) {

		String[] re = new String[num];

		for (int i = 0; i < num; ++i) {
			re[i] = createUuid();
		}

		return re;
	}

	/**
	 * @Description description
	 */
	public static String generateUID() {
		int max = 99999999;
		int min = 10000000;

		Random random = new Random();
		int rand = random.nextInt(max) % (max - min + 1) + min;
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmssSSS");
		String sf = sdf.format(ThreadLocalModel.getThreadLocal().getNow());
		/* 返回串 时间（13） + ip(6) + port(5) + random(6) */
		return "BOSS" + sf + CommonConst.SEQ_VAL + String.format("%08d", rand);
	}
	
	public static String getDateBySN(String paySn) {
		int length = paySn.length();
		String result = "";
		if (length > 27 && paySn.startsWith("BOSS")) {
			if (length < 30) {
				String baseStr = paySn.substring(4, 14);
				if (baseStr.startsWith("0")) {
					result = 2016 + baseStr;
				} else {
					result = 2015 + baseStr;
				}
			} else {
				result = 20 + paySn.substring(4, 16);
			}
		}
		return result;
	}
	
	/** */
    public static String geneFreezeUID(long passId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String sf = sdf.format(ThreadLocalModel.getThreadLocal().getNow());
        String random = createUuid();
        /* 返回串 时间（14） + passId(n) +  random(32) */
        return sf + passId + random;
    }
}
