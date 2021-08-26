package com.taoji666.gulimall.search.feign;


import com.taoji666.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


/**
 * @author: Taoji
 * @date: 2021/8/23 10:11
 */
@FeignClient("gulimall-product") //远程调用nacos中注册的gulimall-product微服务
public interface ProductFeignService {

    @GetMapping("/product/attr/info/{attrId}")  //注意，被调用的微服务路径要写完整，类上的路径+方法上的路径
    R attrInfo(@PathVariable("attrId") Long attrId); //提取路径变量，赋值给attrId

    @RequestMapping("/product/brand/infos")
    public R brandsInfo(@RequestParam("brandIds") List<Long> brandIds);
}
