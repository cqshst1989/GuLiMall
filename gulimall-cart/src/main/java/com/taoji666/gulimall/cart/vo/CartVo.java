package com.taoji666.gulimall.cart.vo;


import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description:
 *    `整个`购物车存放的商品信息   需要计算的属性需要重写get方法，保证每次获取属性都会进行计算。和redis交互
 *    本类的属性，都是计算CartItemVo中的属性得出，因此没有set方法，get方法自己写加减乘除运算
 *
 *    需要计算的属性，必须重写get方法，保证每次获取属性都会进行计算
 * @author: Taoji
 * @createTime: 2020-06-30 16:42
 **/
public class CartVo {

    /**
     * 购物车子项信息
     */
    List<CartItemVo> items;

    /**
     * 商品数量  购物车中的所有 商品的数量  （非每一种商品的数量）
     * 因此是一个需要计算得出的数据，非自己设置的数据  计算方法见set方法
     */
    private Integer countNum;

    /**
     * 商品类型数量  有几种商品   比如可乐， 土豆， 牛奶，iphone12  就4种
     */
    private Integer countType;

    /**
     * 所有商品的总价    也是需要自己计算后生成的数据，非录入数据，因此本类也需要
     *
     */
    private BigDecimal totalAmount;

    /**
     * 减免价格    优惠券给用上啊
     */
    private BigDecimal reduce = new BigDecimal("0.00");//初始值赋值为0，没有见面任何价格

    public List<CartItemVo> getItems() {
        return items;
    }

    public void setItems(List<CartItemVo> items) {
        this.items = items;
    }

    //计算购物车中的所有商品数量
    public Integer getCountNum() {
        int count = 0;

        //将每一项的具体商品数量取出，并求和    比如 杯子12个，iphone 2个，西瓜 2个 共计16个
        if (items != null && items.size() > 0) {
            for (CartItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    public Integer getCountType() {
        int count = 0;
        if (items != null && items.size() > 0) {
            for (CartItemVo item : items) {
                count += 1;
            }
        }
        return count;
    }

    //计算所有商品的总价
    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");
        // 1、计算购物项总价
        if (!CollectionUtils.isEmpty(items)) {
            for (CartItemVo cartItem : items) {
                if (cartItem.getCheck()) {
                    BigDecimal totalPrice = cartItem.getTotalPrice();
                    amount = amount.add(totalPrice); //当前购物项的价格，在购物项类中，已经用它的getTotalPrice()计算过了
                }
            }
        }
        // 2、计算优惠后的价格 （-优惠券的价格）
        return amount.subtract(getReduce()); //substract 自带的减法
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
