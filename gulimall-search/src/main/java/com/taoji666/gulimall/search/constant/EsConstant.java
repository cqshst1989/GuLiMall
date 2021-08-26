package com.taoji666.gulimall.search.constant;

/**
 * @author: Taoji
 * @date: 2021/7/26 15:22
 */
public class EsConstant {

    /**
     * 在es中的索引, 已经修改完映射并数据迁移
     */
    public static final String PRODUCT_INDEX = "gulimall_product"; //sku数据在es中的索引

    public static final Integer PRODUCT_PAGE_SIZE = 16; //sku数据Es中的索引，可以决定前端页面每页显示16个商品
}