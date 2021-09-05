package com.taoji666.gulimall.order.to;

import com.taoji666.gulimall.order.entity.OrderEntity;
import com.taoji666.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateTo {

    private OrderEntity order; //订单实体类

    private List<OrderItemEntity> orderItems; //订单项

    /** 订单计算的应付价格 **/
    private BigDecimal payPrice;

    /** 运费 **/
    private BigDecimal fare;

}
