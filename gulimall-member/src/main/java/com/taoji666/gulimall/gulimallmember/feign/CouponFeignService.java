package com.taoji666.gulimall.gulimallmember.feign;

import com.taoji666.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;



/*@FeignClient注解放在类上，说明这是一个远程客户端，将要去调用远程服务。类上写注册进nacos的微服务名
但是这个微服务名会对应很多具体微服务，我们只需要进那个微服务（一般是controller），将要调用的
方法名和方法名上面的注解整个复制出来就好 */
@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    //去coupon工程，将要调用的远程方法及注解粘贴过来
    @RequestMapping("/gulimallcoupon/coupon/member/list")
    public R membercoupons();
}
