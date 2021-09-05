package com.taoji666.gulimall.order.vo;

import com.taoji666.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {

    private OrderEntity order; //订单信息

    /** 错误状态码
     * 0 成功
     * 其他  各种错误信息
     * **/
    private Integer code;
}
