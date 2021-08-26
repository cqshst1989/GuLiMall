package com.taoji666.gulimall.thirdparty.controller;

import com.taoji666.common.utils.R;
import com.taoji666.gulimall.thirdparty.component.SmsComponent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Description: 发送短信验证码
 * @author: TaoJi
 * @createTime: 2021-08-09 22:12
 **/
@RestController //返回json数据，不是返回某个网页
@RequestMapping(value = "/sms")
public class SmsSendController {

    @Resource
    private SmsComponent smsComponent;

    /**
     * 提供给别的微服务进行调用，就是feign过来的，并非用户直接从网页输入网址来访问的
     * @param phone  别的微服务auth-server 发来接收的手机号
     * @param code    和 验证码
     * @return
     */
    @GetMapping(value = "/sendCode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code) {
        //发送验证码
        smsComponent.sendCode(phone,code);
        return R.ok();
    }
}

