package com.taoji666.common.constant;

/**
 * @Description: 购物车常量
 * @author: TaoJi
 * @createTime: 2021-08-25 18:00
 **/
public class CartConstant {

    public final static String TEMP_USER_COOKIE_NAME = "user-key"; //临时用户的 cookie名字

    public final static int TEMP_USER_COOKIE_TIMEOUT = 60*60*24*30; //设置cookie的过期时间，单位是秒。60秒*60分钟*24条*30天

    public final static String CART_PREFIX = "gulimall:cart:"; //购物车信息（临时购物车 or 用户购物车）


}

