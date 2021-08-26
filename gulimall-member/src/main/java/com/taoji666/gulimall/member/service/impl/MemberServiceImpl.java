package com.taoji666.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.taoji666.common.utils.HttpUtils;
import com.taoji666.gulimall.member.dao.MemberDao;
import com.taoji666.gulimall.member.dao.MemberLevelDao;
import com.taoji666.gulimall.member.entity.MemberEntity;
import com.taoji666.gulimall.member.entity.MemberLevelEntity;
import com.taoji666.gulimall.member.exception.PhoneException;
import com.taoji666.gulimall.member.exception.UsernameException;
import com.taoji666.gulimall.member.service.MemberService;
import com.taoji666.gulimall.member.vo.MemberUserLoginVo;
import com.taoji666.gulimall.member.vo.MemberUserRegisterVo;
import com.taoji666.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

/**
 * 会员
 *
 * @author Taoji
 */
@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }
    //注册（第一次） 或 登录 社交账户（比如微博）
    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {

        //获取到 微博 返回的 Uid
        String uid = socialUser.getUid();

        //判断当前社交用户是否已经登录过系统，有就登录，没有就注册

        //1、如果传来的uid 数据库中有，说明已经注册过了，就只需要更新用户的访问令牌access_token和过期时间
        MemberEntity memberEntity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (memberEntity != null) {
            //更新用户的访问令牌access_token和过期时间
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());
            baseMapper.updateById(update);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity; //返回会员注册信息
            /*！！！MemberEntity 有两个对象 update用于更新真数据表，memberEntity用于返回给前端用
            *主要是因为，我要返回给前端用的只有2个信息，给update，给的信息太多了
            */

        //2、没有查到当前社交用户对应的记录我们就需要注册一个
        } else {
            MemberEntity register = new MemberEntity();
            /*2.1 查询当前社交用户的社交账号信息（昵称、性别等）
            * 这里是通过 令牌，去微博开放的 接口  申请查询
            * 微博开放查询的接口，见开发文档：
            *
             */
            try {

            } catch (Exception e){}
            //我们依然使用HTTP Utils来查询
            Map<String, String> query = new HashMap<>();
            query.put("access_token", socialUser.getAccess_token());
            query.put("uid", socialUser.getUid());

            //HttpUtils.doGet(微博主机地址,访问路径，请求方式，请求头，路径参数) 请求头数没有，又不能为空，就new 一个 hashmap。 注意：Get请求没有请求体
            HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<>(), query);

            //响应状态码200,说明查询成功
            if (response.getStatusLine().getStatusCode() == 200) {
                //取出response中的完整内容：即微博所有公开的用户资料，非常多的资料
                String json = EntityUtils.toString(response.getEntity());
                //json字符串转换成json对象，方便提取属性 （百度 Json字符串 和 Json对象）
                /*一般我们要操作json 中的数据的时候，需要json 对象的格式。
                一般我们要在客户端和服务器之间进行数据交换的时候，使用json 字符串。*/
                JSONObject jsonObject = JSON.parseObject(json);

                //对照控制台 从微博回传的那么多内容中，想要存进会员数据表 的参数。根据数据表，这里只要姓名，年龄，和头像
                String name = jsonObject.getString("name");
                String gender = jsonObject.getString("gender");
                String profileImageUrl = jsonObject.getString("profile_image_url");

                //2.2 将获取到的信息更新进会员数据表，完成注册
                register.setNickname(name);  //昵称，展示在首页的： 你好 xx
                register.setGender("m".equals(gender) ? 1 : 0);
                register.setHeader(profileImageUrl);
                register.setCreateTime(new Date());


                register.setSocialUid(socialUser.getUid());
                register.setAccessToken(socialUser.getAccess_token());
                register.setExpiresIn(socialUser.getExpires_in());

                //把用户信息插入到数据库中
                baseMapper.insert(register);
            }
            return register;
        }
    }

    //验证用户提交的 用户名 和 密码 是否正确。并返回用户信息
    @Override
    public MemberEntity login(MemberUserLoginVo vo) {
        //1、获取前端传来的用户名 和 密码
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        //2、去数据库查询 该用户的 用户名和密码  用于和前端传来的作比较

        //2.1、去数据库用户名或者手机号 SELECT * FROM ums_member WHERE username = ? OR mobile = ?
        //selectOne 用户名 或者 手机号 任何一个匹配（都是数据表中唯一的数据）
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>()
                .eq("username", loginacct).or().eq("mobile", loginacct));

        if (memberEntity == null) {
            //登录失败
            return null;
        } else {
            //2.2 数据库中确实有这个用户后，接着验证密码
            // 首先获取到数据库里的password
            String password1 = memberEntity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //用spring的方法  将传入密码 和 数据库中的密码 进行密码匹配
            boolean matches = passwordEncoder.matches(password, password1);
            if (matches) {
                //登录成功
                return memberEntity;
            }
        }

        return null;
    }

    //将新用户的手机，用户名，密码保存进数据表 ums_member
    @Override
    public void register(MemberUserRegisterVo vo) {

        MemberEntity memberEntity = new MemberEntity(); //数据库的Dao 来接收 数据

        //设置默认会员等级  刚注册的新用户，注册等级默认是最低级

        //先从数据库的会员等级表中获取到默认的会员等级是啥
        //自己写个Dao方法来查到会员等级,貌似可以使用现成方法：MemberLevelEntity levelEntity= memberLevelDao.selectOne(new QueryWrapper<MemberLevelEntity>().eq("default_status",1));
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        //将查到的会员等级设置给memberEntity
        memberEntity.setLevelId(levelEntity.getId());

        //设置新人在注册页面中填写的vo中的信息
        //检查用户名和手机号是否唯一。方法里面有感知异常的代码，如果抛了异常，最终在调用的controller中try catch掉
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        memberEntity.setNickname(vo.getUserName());
        memberEntity.setUsername(vo.getUserName());

        /*密码进行MD5加密:MD5具有不可逆性，为了安全，密码还需要加一个“盐值”，再做MD5运算。之后再存入数据库
        盐值：可以是系统当前时间，也可以是自定义的某个数列。spring帮我们封装了一种盐值算法
        BCryptPasswordEncoder类的encode方法，经过该方法计算后，再存入数据库就稳了
        */
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encode = bCryptPasswordEncoder.encode(vo.getPassword());//MD5+盐值 加密
        memberEntity.setPassword(encode); //将密码存入数据库的Entity

        memberEntity.setMobile(vo.getPhone());
        memberEntity.setGender(0);
        memberEntity.setCreateTime(new Date());

        //保存数据，真正插入数据库
        this.baseMapper.insert(memberEntity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneException {

        //MP自带计数方法，selectCount
        Integer phoneCount = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));

        if (phoneCount > 0) { //说明数据库中有这个手机号，即该手机号已经注册过了，抛异常
            throw new PhoneException();
        }

    }

    @Override
    public void checkUserNameUnique(String userName) throws UsernameException {

        Integer usernameCount = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));

        if (usernameCount > 0) {
            throw new UsernameException();
        }
    }

}