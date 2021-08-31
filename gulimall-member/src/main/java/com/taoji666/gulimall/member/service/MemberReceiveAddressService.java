package com.taoji666.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.member.entity.MemberReceiveAddressEntity;

import java.util.List;
import java.util.Map;

/**
 * 会员收货地址
 *
 * @author Taoji
 * @date 2021-08-30 22:50:05
 */
public interface MemberReceiveAddressService extends IService<MemberReceiveAddressEntity> {

    PageUtils queryPage(Map<String, Object> params);


    List<MemberReceiveAddressEntity> getAddressByUserId(Long userId);//获取会员收货地址
}

