package com.taoji666.gulimall.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * @Description: springSession配置类 将默认cookie域放大
 * @author: TaoJi
 * @createTime: 2022-08-30 15:30
 *
 *从官方文档复制出来改cookie的session作用域  修改浏览器f12调试，->application->cookies 中的都可以该
 * 比如domain（作用域） path expires(session过期时间，默认是一次会话）
 * 本示例主要改domain
 *
 * 特别注意：由于是分布式session，必须保证所有微服务的 Session配置都一样，才能正确使用。复制到所有相关Module，可以打包依赖，
 * 但就两个微服务用，就复制粘贴吧，另一个是product
 *
 *
 **/
@Configuration
public class GulimallSessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {

        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();

        //放大作用域
        cookieSerializer.setDomainName("gulimall.com");  //放大作用域到gulimall.com ，任意xx.gulimall.com都能使用到该session
        cookieSerializer.setCookieName("GULISESSION"); //还可以给session取个名字，默认就叫session

        return cookieSerializer;
    }

    //由于从内存存进Redis，有对象 变字节流的过程。 还需要使用JSON的序列化方式来序列化对象到Redis中
    //根据文档，只需要new一个GenericJackson2JsonRedisSerializer()添加进入Bean，即可完成序列化机制
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

}