package com.taoji666.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description: 购物项内容:  购物车中的 子属性， 没写内部类而已
 * @author: TaoJi
 * @createTime: 2021-08-25 09:25
 **/
public class CartItemVo {

    private Long skuId;

    private Boolean check = true; //该项有没有被用户选中。 比如 可口可乐2L

    private String title;

    private String image;

    /**
     * 商品套餐属性
     */
    private List<String> skuAttrValues; //可能有多种属性  比如 iqoo neo5  属性1 256G 属性2 星河银 属性3 5G版

    private BigDecimal price; //价格  涉及小数计算，一定要用BigDecimal，否则算不准的

    private Integer count; //数量

    private BigDecimal totalPrice; //总价是根据 count 和 price 动态计算的。不能被赋值，所以不能用@Data了，但还有IDEA自带的generate getter setter方法

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Boolean getCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getSkuAttrValues() {
        return skuAttrValues;
    }

    public void setSkuAttrValues(List<String> skuAttrValues) {
        this.skuAttrValues = skuAttrValues;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    /**
     * 计算当前购物项总价
     *
     * @return 总价=单价price * 数量， 由于不是基本数据类型，也没得自动拆包，所以用类的自带函数进行计算
     * 把数量也快速转换成BigDecimal
     */
    public BigDecimal getTotalPrice() {

        return this.price.multiply(new BigDecimal("" + this.count));
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }


}
