package com.taoji666.gulimall.gulimallmember.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.gulimallmember.entity.MemberLoginLogEntity;

import java.util.Map;

/**
 * 会员登录记录
 *
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-04 12:56:55
 */
public interface MemberLoginLogService extends IService<MemberLoginLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

