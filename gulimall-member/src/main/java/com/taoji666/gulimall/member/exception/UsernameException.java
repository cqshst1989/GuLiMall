package com.taoji666.gulimall.member.exception;


/**
 * @Description:
 * @author TaoJi
 * @createTime: 2021-08-20 20:04
 **/
public class UsernameException extends RuntimeException {
    public UsernameException() {
        super("存在相同的用户名");
    }
}
