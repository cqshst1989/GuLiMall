/**
 * Copyright 2019 bejson.com
 */
package com.taoji666.gulimall.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Auto-generated: 2021-7-16 10:50:34
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 * 通过网上的json工具，直接将前端传来的json转成java实体类
 * 注意的是：网上自动生成的对小数 不敏感，需要手动修改一些小数属性，由于需要精确运算，因此采用BigDecimal
 * 还有就是，数据库里面的字段对应的都是long，要改
 */
@Data
public class SpuSaveVo {

    private String spuName;
    private String spuDescription;
    private Long catalogId;
    private Long brandId;
    private BigDecimal weight;
    private int publishStatus;
    //以上 商品基本信息  后面的都是 类（另外一张数据表）

    private List<String> decript;
    private List<String> images;
    private Bounds bounds;
    private List<BaseAttrs> baseAttrs;
    private List<Skus> skus;



}