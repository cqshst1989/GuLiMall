package com.taoji666.gulimall.cart.to;

import lombok.Data;
import lombok.ToString;

/**
 * To:说明该数据会在微服务之间传输
 * @Description: 判断用户是否登录，以确定购物车是临时购物车，还是用户购物车

 * @author: TaoJi
 * @createTime: 2021-08-25 17:35
 **/
@ToString
@Data
public class UserInfoTo {

    private Long userId;  //登录就会有用户Id

    private String userKey;  //没登录就会被设置一个临时的userKey

    /**
     * 是否临时用户
     */
    private Boolean tempUser = false;

}
