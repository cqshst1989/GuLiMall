package com.taoji666.gulimall.member.vo;

import lombok.Data;

/**
 * @Description: 认证微服务远程调用这个Member微服务的方法，存储新注册用户
 * @author TaoJi
 * @createTime: 2021-08-20 15:37
 **/

@Data
public class MemberUserRegisterVo {

    private String userName; //新用户的用户名

    private String password; //新用户的用户密码

    private String phone; //新用户的手机号

}

