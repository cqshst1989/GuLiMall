package com.taoji666.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.order.entity.OrderReturnReasonEntity;

import java.util.Map;

/**
 * ้่ดงๅๅ 
 *
 * @author leifengyang
 * @email leifengyang@gmail.com
 * @date 2019-10-08 09:56:16
 */
public interface OrderReturnReasonService extends IService<OrderReturnReasonEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

