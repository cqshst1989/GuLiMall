package com.taoji666.gulimall.order.config;


import com.taoji666.gulimall.order.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/*
* 将写好的  登录拦截器（未登录的用户不能进入订单服务），配置好
* */
@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {
    @Autowired
    LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //所有请求都要被拦截，检查有没有登录
        registry.addInterceptor(loginInterceptor).addPathPatterns("/**");
    }
}
