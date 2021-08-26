package com.taoji666.gulimall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**谷粒商城项目需要检索的东西
 * (1) 方便检索 但是会有冗余字段（attrs会重复）
 * {
 *     skuId:1
 *     skuTitle: 华为xx
 *     price: 998
 *     saleCount: 99
 *     attrs:[
 *        {尺寸:5寸}
 *        {CPU:高通945}
 *        {分辨率：全高清}
 *     ]
 * }
 * （2）单列attr，不会冗余。但是计算量会非常大，大数据下，很浪费时间
 */
@EnableRedisHttpSession //开启springsession，以便list.html使用session的值
@EnableFeignClients //开启远程微服务调用功能
@EnableDiscoveryClient
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class) //本项目没有用数据源，但是pom又配置了数据源，不排除掉，会报错
public class GulimallSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallSearchApplication.class, args);
    }

}
