package com.taoji666.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.taoji666.common.utils.HttpUtils;
import com.taoji666.common.utils.R;
import com.taoji666.common.vo.MemberResponseVo;
import com.taoji666.gulimall.auth.feign.MemberFeignService;
import com.taoji666.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static com.taoji666.common.constant.AuthServerConstant.LOGIN_USER;

/**
 * @Description: 处理社交登录请求:前端login.html向微博的引导地址发送请求后，微博会带着code，来到此方法。（配置的redirect_uri就是这里）
 * @author: Taoji
 * @createTime: 2021-08-21 22:08
 **/

@Slf4j
@Controller
public class OAuth2Controller {
    @Autowired
    private final MemberFeignService memberFeignService;

    @Autowired
    public OAuth2Controller(MemberFeignService memberFeignService) {
        this.memberFeignService = memberFeignService;
    }

    @GetMapping(value = "/oauth2.0/weibo/success")
                       //取出微博返回的Code
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {

        //通过HttpUtils.doPost向微博的开放接口https://api.weibo.com/oauth2/access_token?clint_id= ${code}获取令牌。
        //HttpUtils.doPost需要的请求体参数 先全部放在map里面， 最主要的就是code，其他都直接抄
        Map<String, String> map = new HashMap<>(5);
        map.put("client_id", "2077705774"); //就是App Key
        map.put("client_secret", "40af02bd1c7e435ba6a6e9cd3bf799fd"); //就是App Secret
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code", code);

        //1、根据用户授权返回的code去微博给的URL换取access_token
        // 微博获取令牌接口：https://api.weibo.com/oauth2/access_token?clint_id= ${code}
        //HttpUtils.doPost(微博主机地址,访问路径，请求方式，请求头，请求体,路径参数) 请求头和路径参数没有，又不能为空，就new 一个 hashmap
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", new HashMap<>(), map, new HashMap<>());

        //2、处理
        //response里面就有获取到的令牌access_token，一个json字符串。和SocialUser完全对应
        /* 重要的是，这个令牌封装的json中，有以下三项要添加进数据库（数据库 和 Entity 都要补上）
           {
         *   "access_token":"123fsdfsdf34vx", //唯一令牌，有令牌才能去微博取该用户微博中的数据
         *   "expires_in":157679999  //令牌有效期
         *   "uid":"dfadf" //用户唯一ID，就用这个id注册进我们的会员数据表。也和微博产生关系
         *    .....
         * }
         * */
        if (response.getStatusLine().getStatusCode() == 200) { //HTTP响应码如果是200，就说明访问成功，即获取到了access_token
            //获取到了access_token,转为通用社交登录对象
            String json = EntityUtils.toString(response.getEntity()); //先获取json，再通过EntityUtils工具类转换成Strong
            //方案二：用JSON工具：String json = JSON.toJSONString(response.getEntity());
            //在网上找个JSON转JavaBean的，快速创建与该json配套的VO，然后用JSON.parseObject解析成java对象，赋值给vo
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);


            //1）、当前用户如果是第一次进网站，根据令牌获取的信息，自动注册进ums_member数据表（为当前社交用户生成一个会员信息，以后这个社交账号就对应指定的会员）
            //登录或者注册这个社交用户（第一次就是注册）
            System.out.println(socialUser.getAccess_token());
            //调用远程服务完成此功能
            R oauthLogin = memberFeignService.oauthLogin(socialUser);
            if (oauthLogin.getCode() == 0) { //登录成功
                MemberResponseVo data = oauthLogin.getData("data", new TypeReference<MemberResponseVo>() {
                });
                log.info("登录成功：用户信息：{}", data.toString());//记得给MemberResponseVo 加上toString注解，才能这样打印

                //1、第一次使用session，命令浏览器保存卡号，JSESSIONID这个cookie
                //以后浏览器访问哪个网站就会带上这个网站的cookie
                /*
                * 分布式 session跨域 问题：
                * 1、product微服务的 index.html 前端，想使用auth微服务OAuth2Controller中域对象数据（显示 xx用户已登录）。产生了session跨域问题
                * 2、由于从内存存进Redis，有对象 变字节流的过程。 还需要使用JSON的序列化方式来序列化对象到Redis中
                * 解决方法：
                * 浏览器端cookie，放大cookie的域。 默认cookie的域是当前域名 auth.gulimall.com
                * 将其放大成gulimall.com(GulimallSessionConfig中完成) ，之后，任意 xx.gulimall.com 都会被发服务器给的JSESSIONID
                *特别指出：在浏览器的调试台中，显示的是.gulimall(有个.)
                * 服务端session：
                *   将用户的原来单机版时的session，存进redis
                *
                * 序列化方法，在GulimallSessionConfig中一并完成了
                *
                *
                * */

                session.setAttribute(LOGIN_USER, data);//加入session，供其他微服务调用，这一步需要配置类，启动类上的注解，pom依赖，配置文件都做好才行

                //2、登录成功跳回首页
                return "redirect:http://gulimall.com";
            } else { //失败，就返回登录页面，重新登录
                return "redirect:http://auth.gulimall.com/login.html";
            }
        } else { //获取令牌失败，就返回登录页，重新登录
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }

}
