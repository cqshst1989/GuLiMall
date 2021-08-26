package com.taoji666.gulimall.member.vo;

import lombok.Data;

/**
 * @Description:前端新用户提交过来的用户名和密码
 * @author TaoJi
 * @createTime: 2021-08-21 16:30
 **/

@Data
public class MemberUserLoginVo {

    private String loginacct;

    private String password;

}

