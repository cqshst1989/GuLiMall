package com.taoji666.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 二级分类的vo，首页分类数据,查到后给catalogLoader.js用于组装
 *
 * @author: Taoji
 * @date: 2021/7/30 19:17
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catalog2Vo {


    private String id;

    private String name;

    /**
     * 一级父分类的id
     */
    private String catalog1Id;


    /**
     * 三级子分类  静态内部类
     */
    private List<Category3Vo> catalog3List;


    /**
     * 三级分类vo
     * "catalog2Id":"1",
     *     "id":"1"
     *     "name":"电子书"
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Category3Vo {

        /**
         * 父分类、二级分类id
         */
        private String catalog2Id;

        private String id;

        private String name;
    }

}
