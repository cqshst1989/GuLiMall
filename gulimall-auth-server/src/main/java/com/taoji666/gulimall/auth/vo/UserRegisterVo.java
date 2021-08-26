package com.taoji666.gulimall.auth.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * @author: Taoji
 * @createTime: 2021-08-20 10:39
 *
 * 前端传来的用户注册信息
 *
 * @NotEmpty 服务端验证，不能为空，一旦为空，就返回异常 并返回message内容
 **/

@Data
public class UserRegisterVo {

    @NotEmpty(message = "用户名不能为空") //服务端验证
    @Length(min = 6, max = 19, message="用户名长度在6-18字符")
    private String userName;

    @NotEmpty(message = "密码必须填写")
    @Length(min = 6,max = 18,message = "密码必须是6—18位字符")
    private String password;

    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^[1]([3-9])[0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    @NotEmpty(message = "验证码不能为空")
    private String code;

}
