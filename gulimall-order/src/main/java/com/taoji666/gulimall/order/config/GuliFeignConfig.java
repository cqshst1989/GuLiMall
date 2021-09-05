package com.taoji666.gulimall.order.config;


import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;


/*
* 1、feign在进行远程调用的时候，默认情况，会丢掉请求头（类似nginx），这样如果请求头中带有 cookie 登录信息，也会被丢失
*
* 好在feign允许我们自行配置 feign的拦截器，加入spring容器即可，源码会按顺序调用各个拦截器
*
* 我们就可以在拦截器中写代码，加上请求头中的 cookie
*
* */
@Configuration
public class GuliFeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                //1. 使用RequestContextHolder拿到当前请求 的 请求数据
                /*
                * 但是RequestContextHolder底层源码是从ThreadLocal中获取 请求，如果feign的调用在 `异步` 线程中，就会丢失请求
                * 因此一定要在feign调用前，解决这个问题，否则这里就拿不到请求。详情参见
                * */
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (requestAttributes != null) {
                    HttpServletRequest request = requestAttributes.getRequest(); //拿到请求数据request

                    //如果请求数据不为空，就从请求头中取出cookie，并放入feign的新请求
                    if (request != null) {
                        //2. 取出原请求中的cookie信息
                        String cookie = request.getHeader("Cookie");
                        //3、放入新的feign请求  这样登录信息就不会丢失了
                        template.header("Cookie", cookie);
                    }
                }
            }
        };
    }
}