package com.taoji666.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.taoji666.common.exception.BizCodeEnume;
import com.taoji666.gulimall.member.exception.PhoneException;
import com.taoji666.gulimall.member.exception.UsernameException;
import com.taoji666.gulimall.member.feign.CouponFeignService;
import com.taoji666.gulimall.member.entity.MemberEntity;
import com.taoji666.gulimall.member.service.MemberService;
import com.taoji666.gulimall.member.vo.MemberUserLoginVo;
import com.taoji666.gulimall.member.vo.MemberUserRegisterVo;
import com.taoji666.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * 会员
 *
 * @author TaoJi
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    CouponFeignService couponFeignService;

    @PostMapping(value = "/oauth2/login")
    public R oauthLogin(@RequestBody SocialUser socialUser) throws Exception {

        MemberEntity memberEntity = memberService.login(socialUser); //login方法重载

        if (memberEntity != null) {
            return R.ok().setData(memberEntity);
        } else {
            return R.error(BizCodeEnume.LOGIN_ACCOUNT_PASSWORD_EXCEPTION.getCode(), BizCodeEnume.LOGIN_ACCOUNT_PASSWORD_EXCEPTION.getMsg());
        }
    }

    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");

        R membercoupons = couponFeignService.membercoupons();
        return R.ok().put("member",memberEntity).put("coupons",membercoupons.get("coupons"));
    }

    //认证微服务 远程调用的，传来用户的  用户名 和 密码。 在本方法检验 用户名 和密码 是否正确
    @PostMapping(value = "/login")
    public R login(@RequestBody MemberUserLoginVo vo) {

        MemberEntity memberEntity = memberService.login(vo); //登录成功，返回用户信息

        if (memberEntity != null) { //如果返回了用户信息，说明登录成功
            return R.ok().setData(memberEntity);

        } else { //否则 登录失败
            return R.error(BizCodeEnume.LOGIN_ACCOUNT_PASSWORD_EXCEPTION.getCode(), BizCodeEnume.LOGIN_ACCOUNT_PASSWORD_EXCEPTION.getMsg());
        }
    }

    //这个由认证微服务，远程调用该方法，将新用户注册进数据库
    @PostMapping(value = "/register")
    public R register(@RequestBody MemberUserRegisterVo vo) { //@RequestBody将认证微服务传来的json，转成java对象

        try {
            memberService.register(vo); //将新用户注册进数据库。由于register方法会抛异常，抛出的异常，就在这里try catch掉
        } catch (PhoneException e) { //这里写自己定义的异常（直接继承的Runtime异常）
            return R.error(BizCodeEnume.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnume.PHONE_EXIST_EXCEPTION.getMsg());
        } catch (UsernameException e) {
            return R.error(BizCodeEnume.USER_EXIST_EXCEPTION.getCode(), BizCodeEnume.USER_EXIST_EXCEPTION.getMsg());
        }

        return R.ok();
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
