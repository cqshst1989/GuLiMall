package com.taoji666.gulimall.cart.config;

import com.taoji666.gulimall.cart.interceptor.CartInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Description: 配置拦截器CartInterceptor，到底拦截哪些URL请求
 * @author: TaoJi
 * @createTime: 2020-06-30 17:57
 **/

@Configuration
class GulimallWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CartInterceptor())//注册拦截器
                .addPathPatterns("/**"); //所有都拦截
    }
}

