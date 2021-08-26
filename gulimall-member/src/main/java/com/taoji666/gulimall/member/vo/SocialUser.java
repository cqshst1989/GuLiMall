package com.taoji666.gulimall.member.vo;

import lombok.Data;

/**
 * @Description: 社交用户登录，用code得到的access_token
 * @author: TaoJi
 * @createTime: 2021-08-22 11:07
 **/
@Data
public class SocialUser {

    private String access_token; //唯一令牌，有令牌才能去微博取该用户微博中的数据

    private String remind_in; //基本没啥用，就是一个格式

    private long expires_in; //令牌有效期

    private String uid; //用户唯一ID，就用这个id注册进我们的会员数据表。也和微博产生关系

    private String isRealName;

}
