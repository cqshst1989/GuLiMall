package com.taoji666.gulimall.ssoclient.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: TaoJi
 * @date: 2021/8/24 10:00
 */

@Controller//下面的方法，一个返回json，一个返回页面，因此不用@RestController
public class HelloController {

    /**
     * 用户无需登录就可访问 /hello
     *
     * @return
     */
    @ResponseBody //返回json，否则返回jsp或html页面
    @GetMapping(value = "/hello")
    public String hello() {
        return "hello"; //这里是返回真 hello 字符串，不是hello.html页面
    }


    /**
    *用户登录后，才允许访问所有员工
    * @param token 只要去ssoserver登录成功跳回来就会带上
    * */
    //如果是从登录微服务重定向过来的，登录服务还会返回LoginController一个token
    //但是第一次直接访问这个，就没得token，因此为了避免混淆，@RequestParam需要用value明确是取token，但token不是必须参数，第一次访问就没有，
    //因此还需要required = false
    @GetMapping(value = "/employees")
    public String employees(Model model, HttpSession session,
                            @RequestParam(value = "token", required = false) String token) {

        //只要token不为空，就说明是从LoginController登录后，跳回来的

        if (!StringUtils.isEmpty(token)) {

            //查询传来的token对应的是哪个用户
            RestTemplate restTemplate = new RestTemplate();

            //将用户存入Redis的时候，就是用String存的（键LoginController的doLogin方法），现在自然用String取出
            ResponseEntity<String> forEntity = restTemplate.getForEntity("http://localhost:8080/userinfo?token=" + token, String.class);
            String body = forEntity.getBody();

            session.setAttribute("loginUser", body);
        }
        Object loginUser = session.getAttribute("loginUser");


        //如果没有登录，就重定向到登录服务器
        // 跳转到登录服务器后. 通过+后面的重定向路径（本微服务首页）， 让登录服务器知道该跳转回到哪里。  LoginController中用@RequestParam取出该参数
        if (loginUser == null) {
            return "redirect:" + "http://localhost:8080/login.html" + "?redirect_url=http://localhost:8081/employees";
        } else {
            List<String> emps = new ArrayList<>();

            emps.add("张三");
            emps.add("李四");

            model.addAttribute("emps", emps);
            return "employees";
        }
    }

}

