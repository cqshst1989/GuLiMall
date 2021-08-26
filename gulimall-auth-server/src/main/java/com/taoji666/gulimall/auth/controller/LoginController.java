package com.taoji666.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.taoji666.common.constant.AuthServerConstant;
import com.taoji666.common.exception.BizCodeEnume;
import com.taoji666.common.utils.R;
import com.taoji666.common.vo.MemberResponseVo;
import com.taoji666.gulimall.auth.feign.MemberFeignService;
import com.taoji666.gulimall.auth.feign.ThirdPartFeignService;
import com.taoji666.gulimall.auth.vo.UserLoginVo;
import com.taoji666.gulimall.auth.vo.UserRegisterVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.taoji666.common.constant.AuthServerConstant.LOGIN_USER;

/**
 * @author: Taoji
 * @createTime: 2021-08-19 10:37
 **/

@Controller
public class LoginController {

    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //前端点击发送验证码，来到本服务
    @ResponseBody //返回json数据 ，类上不是@RestController
    @GetMapping(value = "/sms/sendCode") //前端只用传来手机号码就行，阿里云给该手机号发验证码
    public R sendCode(@RequestParam("phone") String phone) {

        //1、接口防刷：由于接口的url暴露，黑客可能会用post一直刷验证码，

        //从redis中读取验证码
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);

        //一定要有判断不为空，再做防60是操作，否则，redis里面是空的做60s操作，就是空指针异常
        if (!StringUtils.isEmpty(redisCode)) {
            //取出验证码的过期时间：分割redis的值，根据2 _后面的是 将验证码存入系统时的时间
            long currentTime = Long.parseLong(redisCode.split("_")[1]);

            //用当前时间减去存入redis的时间，判断用户手机号是否在60s内发送验证码
            if (System.currentTimeMillis() - currentTime < 60000) {
                //60s内不能再发
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        //2、验证码的再次效验。将验证码存入 redis.存key-phone手机号,value-code验证码

        //生成一个随机验证码
        int code = (int) ((Math.random() * 9 + 1) * 100000);
        String codeNum = String.valueOf(code);
        //System.currentTimeMillis()当前系统时间，用于防止还没到60s就再次发送
        String redisStorage = codeNum + "_" + System.currentTimeMillis();

        //将验证码存入redis，防止同一个手机号在60秒内再次发送验证码  sms:code:189080xxxx（key）
        //虽然手机号码本来就唯一，加前缀就是了redis中数据更整齐，和其他redis数据一起清晰明了
        //向redis数据库中存储值 set(key,value,时间,时间单位)
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone,
                redisStorage, 10, TimeUnit.MINUTES);

        thirdPartFeignService.sendCode(phone, codeNum); //远程调用

        return R.ok();
    }


    /**
     * 注册
     * 获取验证码，填完基本个人信息，点击立即注册以后，注册进数据库
     *
     * Redirect：重定向，相当于办完事情后，让页面重新请求重定向的地址。重定向 浏览器的URL路径会改变，好处就是 前端刷新的页面的时候，由于URL路径变成新的了，
     * 就不会重复提交数据
     * 但是重定向的问题：  不保存数据，我们既想用重定向 解决刷新页面问题，又想能像转发一样携带数据。
     *
     * 解决办法：RedirectAttributes
    原理 : 重定向携带数据：利用session原理，将数据放在session中。
     只要跳转到下一个页面取出，这个数据以后，session里面的数据就会删掉
     *
     * RedirectAttributes：重定向也可以保留数据，不会丢失，但是只能自己项目。重定向去百度，自然百度没有我们的数据
     *
     * @return
     */
    @PostMapping(value = "/register")
    //@Valid 服务端验证注解，验证UserRegisterVo，验证方法再vo里。用了数据校验以后，后面就可以跟BindingResult，校验结果
    //RedirectAttributes attributes,让重定向也能携带数据
    public String register(@Valid UserRegisterVo vos, BindingResult result, RedirectAttributes attributes) {
        //1、服务端验证：看传来的用户名，密码长度等是否符合要求，如果有错误回到注册页面
        if (result.hasErrors()) {
            /*如果有错误，将这些错误从BindingResult中取出封装进Map*/

            /*
            * result.getFieldErrors().stream().collect(Collectors.toMap(fieldError ->{
            *   return fieldError.getField();
            * },fieldError -> {
            *   return fieldError.getDefaultMessage();
            * }))
            * fieldError 是 FieldError 的对象，BindingResult中的错误信息存放在该FieldError对象中，
            * 分别取出该对象的 id（vo属性）和 message（注解上的message） 作为Map的key 和 value
            *
            * 由于只有一个形参，因此可以再次简化Lambda表达式如下：
            * */
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

            attributes.addFlashAttribute("errors", errors);//添加进RedirectAttributes，flash就说明，就一次，下一次刷新（跳到下一个页面），就没得数据了

            //在GulimallWebConfig 配置了简写，可redirect /req.html 但是这么做有问题，前面会直接翻译成本服务ip+端口，不会按照域名写，就不会过nginx会导致静态资源访问不到
            //因此还是写全
            return "redirect:http://auth.gulimall.com/reg.html";//效验出错回到注册页面，重填
        }

        // 2、传来的数据没问题，就可以真正将用户信息注册进数据库

        //2.1 校验验证码：真正注册之前还要看下验证码对不对
        String code = vos.getCode();//先获取页面传来的验证码，准备和redis中的验证码做对比



        //2.2 获取存在Redis里的验证码，如果没过期的话
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vos.getPhone());

        //判定redis中的验证码是否为空（即验证码是够过期）
        if (!StringUtils.isEmpty(redisCode)) {
            //如果验证码不为空，并且是对的。  根据自己的存储方式[0]是字符串 [1]是过期时间
            if (code.equals(redisCode.split("_")[0])) {
                //删除验证码;令牌机制：验证本来就只用一次，用完了当然要删除，先删除，免得后面忘了
                stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vos.getPhone());
                //验证码通过，真正注册，调用远程服务进行注册
                R register = memberFeignService.register(vos);
                if (register.getCode() == 0) {
                    //成功
                    return "redirect:http://auth.gulimall.com/login.html";
                } else { //如果验证不通过，跟写错验证码一样，还是返回注册页面
                    //失败
                    Map<String, String> errors = new HashMap<>();
                    //R中的getData，取出key，并将其转换为String
                    errors.put("msg", register.getData("msg", new TypeReference<String>() {
                    }));
                    attributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else { //验证码写错了，就返回注册页

                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                attributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            //效验出错回到注册页面
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    //如果前端直接敲登录页/login.html 需要判定是否已经登录，如果登录了，就去首页，没有登录才要求登录
    @GetMapping(value = "/login.html")
    public String loginPage(HttpSession session) {

        //从session先取出来用户的信息，判断用户是否已经登录过了,有用户信息就登录过，没有用户信息，就还没有登录
        Object attribute = session.getAttribute(LOGIN_USER);
        //如果用户没登录那就跳转到登录页面
        if (attribute == null) {

            /*知识点插入： 转发 与 重定向
            直接return，就让转发，会拼上你在视图解析器中配置的前缀和后缀
            return xx 相当于 return "forward://完整路径版xx"
            转发 前端页面的URL不变，但是可以保存数据*/

            return "login";
        } else { //有登录信息，就直接去首页

            return "redirect:http://gulimall.com";//重定向，新的一次请求到gulimall去，所有数据不共享（和以上代码无关），除非用特殊手段
        }
    }


   /* @requestbody的用法：

　　1、application/x-www-form-urlencoded

　　　　@requestbody能解析，但springmvc会进行解析，所以通常不用@requestbody。

            　　2、multipart/form-data

　　　　@requestbody不能解析

　　3、application/json，application/xml等：微服务之间传送对象，都是json

　　　　@requestbody能解析，springmvc不会进行解析，所以必须要加@requestbody注解


　　一句话概括：@requestbody能解析json等格式，springmvc只能解析原生表单*/

    //用户填写好 用户名 和 密码 以后，点击登录，发送/login请求，来到本服务
    @PostMapping(value = "/login") //提交的不是json，就是表单 的key和value，springmvc会解析，因此不需要@RequestBody
    public String login(UserLoginVo vo, RedirectAttributes attributes, HttpSession session) {//配置好springsession以后，session就有了分布式功能

        //远程登录
        R login = memberFeignService.login(vo);

        if (login.getCode() == 0) { //说明登录成功
            MemberResponseVo data = login.getData("data", new TypeReference<MemberResponseVo>() {
            });
            //登录成功后，将会员信息放到session中，其他微服务的前端就可以用
            session.setAttribute(LOGIN_USER, data);
            return "redirect:http://gulimall.com";  //注册成功，返回首页
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>() {
            }));
            //RedirectAttributes使重定向也可以携带错误信息，存进去，给前端用一次（flash）
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }


    // @GetMapping(value = "/loguot.html")
    // public String logout(HttpServletRequest request) {
    //     request.getSession().removeAttribute(LOGIN_USER);
    //     request.getSession().invalidate();
    //     return "redirect:http://gulimall.com";
    // }

}
