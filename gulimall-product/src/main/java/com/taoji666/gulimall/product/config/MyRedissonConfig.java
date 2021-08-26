package com.taoji666.gulimall.product.config;




import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author: Taoji
 * @create: 2021-08-3 09:39
 *
 * //参考官网https://github.com/redisson/redisson/wiki，的程序化方式（代码）配置，可自行研究配置文件方式配置
 **/

@Configuration
public class MyRedissonConfig {

    /**
     * 所有对 Redisson 的使用都是通过 RedissonClient
     *
     * @return
     * @throws IOException
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() throws IOException {
        // 1、创建配置
        Config config = new Config();
        // Redis url should start with redis:// or rediss://
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");

        // 2、根据 Config 创建出 RedissonClient 实例
        return Redisson.create(config);
    }

}