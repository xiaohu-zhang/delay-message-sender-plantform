package com.cmcc.timer.mgr.util;

/**
 * @Type CUUID.java
 * @Desc 沿用原mgr工具类
 * @author tab_xu@qq.com
 * @date 2017年3月13日 下午4:36:00
 * @version 1.0
 */
public final class CommonConst {
	/* 操作流水生成序列配置，一个JVM配置一个值，从S01开始，S02,S03... */
	public static final String SEQ_VAL = "S01";
	/* 发行账户的companyId */
	public static final String ISSUE_ACCOUNT_COMPANY_ID = "00";
	
	/*校验公司ID*/
	public static final String CHECK_COMPANYID = "^\\d*$";
	/* AC_CONTRACT_{XXX} */
	public static final int CONTRACT_MOD = 150;

	/* BAL_COMACCTBOOK_{XX} */
	public static final int COM_ACCTBOOK_MOD = 10;

	/* BAL_PAYMENT_{YYYYMMDDX} */
	public static final int PAYMENT_MOD = 10;
	/* BAL_ACCTBOOK_{XX} */
	public static final int ACCTBOOK_MOD = 100;
	/* 现金余额失效时间 */
	public static final String END_TIME = "2099-12-31 23:59:59";
	public static final String CHECK_COMPANYIDS ="^\\[\\\".+\\\"\\]$";//公司集示例["02000001"]
	/* 业务开始时间 */
	public static final String BUSI_BEGINYM = "201508";
	public static final String CREATE_COMPANYID = "01000001";
	public static final String YUNYING_COMPANYID = "99999999";
	public static final String FLOW_COMPANYID= "02000001";
	public final static int JIHEHISTORYNUM = 150;//稽核记录表分表数

}