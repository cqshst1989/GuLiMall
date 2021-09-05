package com.taoji666.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

//封装 前端订单页面  提交过来的数据
@Data
public class OrderSubmitVo {

    /** 收获地址的id **/
    private Long addrId;

    /** 支付方式 **/
    private Integer payType;


    //无需提交要购买的商品，去购物车再获取一遍，这样才是实时计算。主要怕商家刚好在我们提交的时候，调价
    //优惠、发票

    /** 防重令牌 **/
    private String orderToken;

    /** 应付价格  比较前端提交的价格 和 重新算的价格，如果不一样，提示客户价格有变 **/
    private BigDecimal payPrice;

    /** 订单备注 **/
    private String remarks;

    //用户相关的信息，直接去session中取出即可
}
