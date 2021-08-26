package com.taoji666.gulimall.member.exception;

/**
 * @Description:
 * @author Taoji
 * @createTime: 2021-08-20 16:04
 **/
public class PhoneException extends RuntimeException {

    //super：可以存入异常消息
    public PhoneException() {
        super("存在相同的手机号");
    }
}
