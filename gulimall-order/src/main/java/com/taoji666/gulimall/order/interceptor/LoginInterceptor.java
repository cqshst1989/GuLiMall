package com.taoji666.gulimall.order.interceptor;

import com.taoji666.common.constant.AuthServerConstant;
import com.taoji666.common.vo.MemberResponseVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器，未登录的用户不能进入订单服务
 *
 * 订单服务，自然只会允许登录用户享受，为了这个拦截器起作用，还要 配置 一个webMvcConfigurer实现类
 * 将写好的拦截器，添加进webMvcConfigurer实现类
 */

@Component //加入容器，才能在webMvcConfigurer 中自动装配，否则，就得new一个（new一个也没得问题）
public class LoginInterceptor implements HandlerInterceptor {

    //将登录用户的信息放在线程里面，方便同一线程的其他 方法 调用用户信息
    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        AntPathMatcher matcher = new AntPathMatcher();
        boolean match1 = matcher.match("/order/order/infoByOrderSn/**", requestURI);
        boolean match2 = matcher.match("/payed/**", requestURI);
        if (match1||match2) return true;


        //通过springsession 从 redis中 获取用户登录信息
        HttpSession session = request.getSession();
        MemberResponseVo memberResponseVo = (MemberResponseVo) session.getAttribute(AuthServerConstant.LOGIN_USER);

        //如果登录了，就可以去目标方法
        if (memberResponseVo != null) {
            loginUser.set(memberResponseVo); //将内容 设置进 线程
            return true; //放行

        //否则，重定向到登录页面
        }else {
            session.setAttribute("msg","请先登录"); //给一个提示消息，先登录才能进去
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
