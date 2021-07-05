package com.taoji666.gulimall.gulimallorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.gulimallorder.entity.OrderEntity;

import java.util.Map;

/**
 * 订单
 *
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-04 11:43:11
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

