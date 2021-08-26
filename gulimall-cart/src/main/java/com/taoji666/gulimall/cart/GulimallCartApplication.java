package com.taoji666.gulimall.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/*
* 1、购物车需求
* 1)、需求描述:用户可以在`登录状态`下将商品添加到购物车【用户购物车/在线购物车】
* 数据库选型：购物车是读写都很频繁的，因此使用redis
* 放入MySql  （否定）
* 放入mongodb （否定）
* 放入redis (采用)
* 登录以后,会将·临时购物车·的数据全部合并过来,并清空·临时购物车·;
*
* 用户可以在`未登录`状态下将商品添加到购物车【游客购物车/高线购物车/临时购物车】
* 数据库选型：前端数据库localstorage，cookie，WebSql。很好的减轻了服务端压力，但是前端做的，后端拿不到数据，不利于客户购物喜好的大数据分析
* 因此依然使用redis (采用)，这样即使浏览器即使关闭,下次进入,临时购物车数据都在
*
* 用户功能：
* 用户可以使用购物车一起结算下单
* 给购物车添加商品
* 用户可以查询自己的购物车
* 用户可以在购物车中修改购买商品的数量
* 用户可以在购物车中删除商品
* 用户可以一次选择多件商品来批量删除
* 购物车中展示商品的优惠信息
* 提示购物车中价格商品变化
*
 * */

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class) //暂时排除数据库自动配置
public class GulimallCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallCartApplication.class, args);
    }

}
