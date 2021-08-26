package com.taoji666.gulimall.cart.interceptor;


import com.taoji666.common.vo.MemberResponseVo;
import com.taoji666.gulimall.cart.to.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

import static com.taoji666.common.constant.AuthServerConstant.LOGIN_USER;
import static com.taoji666.common.constant.CartConstant.TEMP_USER_COOKIE_NAME;
import static com.taoji666.common.constant.CartConstant.TEMP_USER_COOKIE_TIMEOUT;

/**
 * @Description:
 *    在执行目标方法(CartController的cartListPage方法：进入购物车页面)之前，
 *    判断用户的登录状态.并封装传递给controller目标请求
 * @author: TaoJi
 * @createTime: 2021-08-25 17:15
 **/

//拦截器是拦截 哪个controller呢? 就需要去配置
public class CartInterceptor implements HandlerInterceptor {

    //ThreadLocal用于同一线程数据共享。 拦截器 和 controller 都在一个线程中， 现在controller就想用拦截器中的某些数据
   //由于保存的是UserInfoTo，因此就是这个泛型（待会本方法放进去的是这个UserInfoTo。后面别的Controller取出的也是这个UserInfoTo）
    public static ThreadLocal<UserInfoTo> toThreadLocal = new ThreadLocal<>();

    /***
     * 目标方法执行之前，先执行这个preHandle方法
     * @param request: 主要是通过request 获取到 session 和 cookie
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfoTo userInfoTo = new UserInfoTo();
        //认证服务用spring-session  广发session（session中封装了MemberResponseVo），每个微服务都可以通过request取出
        HttpSession session = request.getSession();
        //获得当前登录用户的信息
        MemberResponseVo memberResponseVo = (MemberResponseVo) session.getAttribute(LOGIN_USER);

        if (memberResponseVo != null) {  //用户登录了

            userInfoTo.setUserId(memberResponseVo.getId()); //将用户id取出给userInfoTo
        }
        //取出cookie ，cookie有很多，自然是数组
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                //user-key
                String name = cookie.getName();
                if (name.equals(TEMP_USER_COOKIE_NAME)) { //common微服务中的constant中的user-key
                    userInfoTo.setUserKey(cookie.getValue()); //取出user-key的值并放进userInfoTo
                    //标记为已是临时用户，，false就代表是注册用户
                    userInfoTo.setTempUser(true);
                }
            }
        }

        //如果没有临时用户一定分配一个临时用户
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }

        //目标方法执行之前，将userInfoTo放进ThreadLocal。方便CartController的目标方法cartListPage取出
        toThreadLocal.set(userInfoTo);


        return true; // 放行
    }


    /**
     * 业务执行之后，第一次登陆时，UUID.randomUUID()生成的临时用户，要保存到浏览器的cookie中
     *
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

        //从线程中获取当前用户的值
        UserInfoTo userInfoTo = toThreadLocal.get();

        //如果是第一次登陆的临时用户，就要保存这个临时用户
        if (!userInfoTo.getTempUser()) {
            //创建一个cookie
            Cookie cookie = new Cookie(TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            //扩大作用域
            cookie.setDomain("gulimall.com");
            //设置过期时间，单位是s
            cookie.setMaxAge(TEMP_USER_COOKIE_TIMEOUT);
            //命令浏览器，保存cookie
            response.addCookie(cookie);
        }

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
