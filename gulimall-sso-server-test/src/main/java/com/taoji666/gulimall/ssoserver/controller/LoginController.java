package com.taoji666.gulimall.ssoserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @author: TaoJi
 * @date: 2021/08/24 10:00
 */
@Controller
public class LoginController {

    @Autowired
    StringRedisTemplate redisTemplate;


    //由HelloController调用，登录成功后通过传token过来，查询真正对应的用户信息，确认到底是哪个用户在登录。
    @ResponseBody
    @GetMapping("/userinfo")
    public String userinfo(@RequestParam(value = "token") String token) {
        return redisTemplate.opsForValue().get(token);

    }

    //浏览器直接访问某个限制资源，还没登录。就被重定向到这里认证服务完成登录，并在URL的参数redirect_url中传来了回传地址 url
    @GetMapping("/login.html")
    public String loginPage(@RequestParam("redirect_url") String url, Model model,
                            //前面已经设置，登录后会被服务器set一个cookie，下次访问的时候，就会自动带着这个cookie
                            //@CookieValue可以取出这个cookie的值，同理由于第一次没有cookie，因此不是必须required = false
                            @CookieValue(value = "sso_token", required = false) String sso_token) {

        //如果cookie不为空，说明已经登录过了，可以重定向回本页面，并且带上token（token里面有uuid，uuid里面有用户名username）
        if (!StringUtils.isEmpty(sso_token)) {
            return "redirect:" + url + "?token=" + sso_token;
        }
        model.addAttribute("url", url);
        return "login";
    }
    //接收表单提交的用户，和密码，完成登录
    @PostMapping(value = "/doLogin")//@RequestParam 取出表单参数
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          @RequestParam("redirect_url") String url,
                          HttpServletResponse response) { //用这个response，来给浏览器添加一个cookie

        //登录成功跳转，给访问权限，跳回到之前的访问页
        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            //生成一个随机数，作为令牌 token，作为已经成功登陆的标志（登陆成功的才有token）  replace：去掉随机数中的-
            String uuid = UUID.randomUUID().toString().replace("-", "");

            //将令牌存入redis
            redisTemplate.opsForValue().set(uuid, username);
            Cookie ssoToken = new Cookie("sso_token", uuid);

            //浏览器有了cookie，下次访问集团内其他系统的时候，带上这个cookie，也算你登录过了。这样就完成多系统登录
            //cookie，浏览器访问某域名的时候，都会默认带上该域名下的所有cookie
            response.addCookie(ssoToken);

            return "redirect:" + url + "?token=" + uuid;
        }
        //登录失败，返回登录页
        return "login";
    }
}

