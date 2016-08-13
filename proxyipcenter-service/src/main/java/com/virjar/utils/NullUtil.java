package com.virjar.utils;

import java.util.Collection;
import java.util.Map;

/**
 * 空对象判断工具
 */
public final class NullUtil {
	
	private NullUtil(){}

	/**
	 * 判断传入的对象是否为空
	 * 
	 * @param obj 数据对象
	 * @return 空则返回true，非空则返回false
	 */
	public static boolean isNull(Object obj) {
		return obj == null?true:false;
	}
	
	/**
	 * 判断传入的字符串是否为空
	 * 
	 * @param obj 数据对象
	 * @return 空则返回true，非空则返回false
	 */
	public static boolean isNull(String obj) {
		return (obj==null||"".equals(obj.trim()))?true:false;
	}
	
	/**
	 * 判断传入的集合是否为空
	 * 
	 * @param obj 数据对象
	 * @return 空则返回true，非空则返回false
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isNull(Collection obj) {
		return (obj==null||obj.size()==0)?true:false;
	}

	/**
	 * 判断传入的Map是否为空
	 * 
	 * @param obj 数据对象
	 * @return 空则返回true，非空则返回false
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isNull(Map obj) {
		return (obj==null||obj.size()==0)?true:false;
	}

	/**
	 * 严格的检查每一个对象是否有为空的情况以及数组是否为空
	 * 
	 * @param objs 数据对象
	 * @return 只要有一个为空就返回true，全都不为空则返回false
	 */
	public static boolean isNull(Object[] objs) {
		boolean flag = false;
		if(objs == null || objs.length == 0){
			flag = true;
			return flag;
		}
		for(Object obj:objs){
			flag = isNull(obj);
			if(flag)break;
		}
		
		return flag;
	}
	
	/**
	 * 严格的检查每一个字符串值是否有为空的情况以及数组是否为空
	 * 
	 * @param objs 数据对象
	 * @return 只要有一个为空就返回true，全都不为空则返回false
	 */
	public static boolean isNull(String ... objs) {
		boolean flag = false;
		if(objs == null || objs.length == 0){
			flag = true;
			return flag;
		}
		for(String obj:objs){
			flag = isNull(obj);
			if(flag)break;
		}
		
		return flag;
	}
	
}
