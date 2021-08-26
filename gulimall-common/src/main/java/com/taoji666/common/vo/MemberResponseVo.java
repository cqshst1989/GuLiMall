package com.taoji666.common.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 会员信息，直接从MemberEntity复制即可
 * 该vo会被auth微服务存入http域，再由product微服务的前端index.html取出
 * @Created: with IntelliJ IDEA.
 * @author: Taoji
 **/

@ToString
@Data
/*由于该会员信息要从内存中存入redis，以解决session跨域问题。因此有一个java对象转二进制流的过程
* 所以一定要实现Serializable接口，只需要写implements Serializable，接口有默认方法，就不需要在该类重写方法了*/
public class MemberResponseVo implements Serializable {

    private static final long serialVersionUID = 5573669251256409786L;

    private Long id;
    /**
     * 会员等级id
     */
    private Long levelId;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 手机号码
     */
    private String mobile;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 头像
     */
    private String header;
    /**
     * 性别
     */
    private Integer gender;
    /**
     * 生日
     */
    private Date birth;
    /**
     * 所在城市
     */
    private String city;
    /**
     * 职业
     */
    private String job;
    /**
     * 个性签名
     */
    private String sign;
    /**
     * 用户来源
     */
    private Integer sourceType;
    /**
     * 积分
     */
    private Integer integration;
    /**
     * 成长值
     */
    private Integer growth;
    /**
     * 启用状态
     */
    private Integer status;
    /**
     * 注册时间
     */
    private Date createTime;

    /**
     * 社交登录UID
     */
    private String socialUid;

    /**
     * 社交登录TOKEN
     */
    private String accessToken;

    /**
     * 社交登录过期时间
     */
    private long expiresIn;

}
