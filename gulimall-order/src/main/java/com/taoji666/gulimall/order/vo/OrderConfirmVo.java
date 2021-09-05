package com.taoji666.gulimall.order.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
/*
* @Data 是用来批量 Getter 和 Setter
*
* */

//返回给前端的  `订单确认页` 要使用的数据   对照京东的订单页面看，就知道要哪些了
public class OrderConfirmVo {

    @Getter
    @Setter
    /** 会员收获地址列表 **/
    private List<MemberAddressVo> memberAddressVos;

    @Getter @Setter
    /** 所有选中的购物项 基本上复制购物项cartItemVo **/
    private List<OrderItemVo> items;

    /** 发票记录 **/
    @Getter @Setter
    /** 优惠券（会员服务里面的会员积分） **/
    private Integer integration;

    /** 防重令牌：防止由于网速不好，用户一直点击提交，从而导致重复提交*/
    @Getter @Setter
    private String orderToken;

    @Getter @Setter
    Map<Long,Boolean> stocks;

    //public Integer count;//商品数量，是计算出来的因此没有Getter，和Setter。特别的：没有这个属性，也可以直接用



    /*
    * 只要有get方法，就算没声明属性，也会自动有getXXX 的XXX属性，所以以后需要计算的属性，都不声明了，直接写get方法算实在
    *
    * 声明了的话，还要再controller里面调 get方法，要麻烦一点
    *
    * */

    public Integer getCount() {  //前端thymeleaf直接使用count的话，相当于直接执行这个方法
        Integer count = 0;
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }



    //计算订单总额 getTotal()就代表订单总额，所以不用写属性名啰嗦了
    //BigDecimal total; //订单总额，需要自己计算，所以不用@Data  ！！ 前端没有total属性，也直接用total了
    public BigDecimal getTotal() {
        BigDecimal totalNum = BigDecimal.ZERO;
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                //计算当前商品的总价格
                BigDecimal itemPrice = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                //再计算全部商品的总价格
                totalNum = totalNum.add(itemPrice);
            }
        }
        return totalNum;
    }


    /** 应付价格 **/
    //BigDecimal payPrice;  //应付价格，需要自己计算
    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
