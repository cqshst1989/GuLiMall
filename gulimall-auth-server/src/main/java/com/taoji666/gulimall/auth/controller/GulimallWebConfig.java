package com.taoji666.gulimall.auth.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * @author: TaoJi
 * @createTime: 2021-08-9 09:44
 *
 * 自定义视图 映射
 **/

@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {
    /**·
     * 视图映射:发送一个请求，直接跳转到一个页面
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        /*
        如果发送一个请求，啥都不做，就直接跳转到某个html页面，直接在这里配置就好，不用写controller了
        *
       在这里写 registry.addViewController("/login.html").setViewName("login");
     相当于在controller里写了
       @GetMapping("/login.html")
        * public String loginPage(){
        *
        *     return "login";
        * }

       如果在return "redirect:/login"  也相当于在web前端直接访问/login.html，因此也可以直接去login页面。
        *
        特别注意，这个方法映射的是Get请求  如果Controller是处理Post请求，处理完后但是最后你转发到这个login来，
        就会造成表单提交post，controller处理post，但是转发过来又是Get，就会报错。

        所以，一旦涉及POST请求，就不要用这个方法啦

        */

        // registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/reg.html").setViewName("reg");
    }
}
