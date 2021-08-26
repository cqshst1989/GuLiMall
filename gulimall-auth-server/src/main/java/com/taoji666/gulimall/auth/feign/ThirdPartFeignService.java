package com.taoji666.gulimall.auth.feign;

import com.taoji666.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



/**
 * @Description: 远程调用第三方微服务 的  短信验证码功能
 * @author: Taoji
 * @createTime: 2021-08-19 22:04
 **/
@FeignClient("gulimall-third-party")
public interface ThirdPartFeignService {

    @GetMapping(value = "/sms/sendCode")
    R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code);

}
