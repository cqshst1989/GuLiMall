package com.taoji666.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.order.entity.OrderEntity;
import com.taoji666.gulimall.order.vo.OrderConfirmVo;
import com.taoji666.gulimall.order.vo.OrderSubmitVo;
import com.taoji666.gulimall.order.vo.SubmitOrderResponseVo;

import java.util.Map;

/**
 * 订单
 *
 * @author Taoji
 * @date 2021-8-30 22:00:00
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder(); //获取订单页面的参数

    SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo); //下单

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity orderEntity);
}

