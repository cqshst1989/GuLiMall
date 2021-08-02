/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 *
 * https://www.renren.io
 *
 * 版权所有，侵权必究！
 */

package com.taoji666.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.http.HttpStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 返回数据
 *
 * @author Mark sunlightcs@gmail.com
 */
public class R extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	//自定义方法
	public R setData(Object data){
		put("data",data);  //类方法， 这里放进去的data 就是  List<SkuHasStockVo> vos
		return this; //返回一个对象，相当于R r = new R(); return r;
	}

	//用的是alibaba的TypeReference  利用fastjson进行逆转.就是转换前面set进去的data
	//方法中的泛型，不是类的泛型
	public <T> T getData(TypeReference<T> typeReference) {
		Object data = get("data");//data就是put进去的  就单独把R中的 List<SkuHasStockVo>取出来，但是取出来以后数据类型不对，后面转换
		String s = JSON.toJSONString(data); //Object转换成String
		T t = JSON.parseObject(s,typeReference); //将String转换成 typeReference 即 List<SkuHasStockVo>
		return t;
	}


	
	public R() {
		put("code", 0);
		put("msg", "success");
	}
	
	public static R error() {
		return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "未知异常，请联系管理员");
	}
	
	public static R error(String msg) {
		return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, msg);
	}
	
	public static R error(int code, String msg) {
		R r = new R();
		r.put("code", code);
		r.put("msg", msg);
		return r;
	}

	public static R ok(String msg) {
		R r = new R();
		r.put("msg", msg);
		return r;
	}
	
	public static R ok(Map<String, Object> map) {
		R r = new R();
		r.putAll(map);
		return r;
	}
	
	public static R ok() {
		return new R();
	}

	public R put(String key, Object value) {
		super.put(key, value);
		return this;
	}
	public  Integer getCode() {

		return (Integer) this.get("code");
	}


}
