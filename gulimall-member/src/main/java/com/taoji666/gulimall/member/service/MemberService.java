package com.taoji666.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.member.entity.MemberEntity;
import com.taoji666.gulimall.member.exception.PhoneException;
import com.taoji666.gulimall.member.exception.UsernameException;
import com.taoji666.gulimall.member.vo.MemberUserLoginVo;
import com.taoji666.gulimall.member.vo.MemberUserRegisterVo;
import com.taoji666.gulimall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author Taoji
 * @date 2021-8-20 16:47:05
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    //新用户注册进数据库
    void register(MemberUserRegisterVo vo);

    /*
     * 判断新用户填写的手机号是否和数据库中的有重复，手机号必须唯一
     *
     * 接口上的异常，就是提醒，这个的实现类会抛异常
     */
    void checkPhoneUnique(String phone) throws PhoneException;

    /*
     * 判断新注册用户的用户名是否重复，用户名必须唯一
     */
    void checkUserNameUnique(String userName) throws UsernameException;

    //前端表单提交的会员注册
    MemberEntity login(MemberUserLoginVo vo);

    //社交用户注册 或 登录
    MemberEntity login(SocialUser socialUser) throws Exception;
}

