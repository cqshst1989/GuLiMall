package com.taoji666.common.constant;



/**
 * @author: TaoJi
 * @date: 2021/8/20 23:30
 *
 * 多个微服务都要用的常量，都放入common
 *
 * 本类是短信验证码的常量
 */
public class AuthServerConstant {
    public static final String SMS_CODE_CACHE_PREFIX = "sms:code:";//手机号前缀，加前缀只是为了redis中格式明确
    public static final String LOGIN_USER = "loginUser";
}
