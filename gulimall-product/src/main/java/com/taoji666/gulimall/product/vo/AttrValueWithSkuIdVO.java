package com.taoji666.gulimall.product.vo;


import lombok.Data;

/**
 * @author Taoji
 * @createTime: 2021-08-15 12:31
 **/
@Data
public class AttrValueWithSkuIdVO {

    private String attrValue;

    private String skuIds; //是skuid，  用逗号分隔。主要是因为mybatis没法支持内部类，又只有一项，就用逗号分离了

}