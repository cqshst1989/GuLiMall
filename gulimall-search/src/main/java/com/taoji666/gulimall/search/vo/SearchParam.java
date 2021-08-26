package com.taoji666.gulimall.search.vo;


import lombok.Data;

import java.util.List;

/**
 * @author: TaoJi
 * @date: 2021/8/9 10:08
 *
 * 本VO封装页面所有可能传递过来的查询条件
 *
 * catalog3Id=225&keyword=小米&sort=saleCount_asc&attrs=1_5寸:8寸&attrs=2_16G:8G
 * 三级分类225，关键字小米 查到后按照销量进行升序，屏幕尺寸5寸和8寸，内存大小16g和8g
 */
@Data
public class SearchParam {

    //keyword 和 calalog3Id是最简单的两个查询，都是一次到位的

    private String keyword; //页面传递过来的全文匹配关键字  (淘宝搜索框里面的字)


    private Long catalog3Id;  //三级分类id（比如手机，电脑，家居）  直接用主页的三级分类ID作为索引条件检索

 /** 下面根据筛选查询（条件查询），比如品牌，价格区间，型号，还可以基于结果再来进行二次筛选（过滤），比如按照价格，销量排序
    前端页面会传来catalog3Id=225&keyword=小米&sort=saleCount_asc
    */


    /*
     * 排序条件：sort=price_desc/asc  按照价格进行升序/降序
     *         sort=salecount_desc/asc   按照销量进行升序/降序
     *         sort=hotscore_desc/asc  按照热度 进行升序/降序
     */
    private String sort;


    // hasStock=0（无库存）/1（有库存）
    private Integer hasStock; //是否显示有货

    /*
     *    价格区间查询
     *    skuPrice=200_500  区间为200-500
     *    skuPrice=_500  区间为0-500
     *    skuPrice=500_  价格区间为500以上
     */
    private String skuPrice;

    /*
     * 品牌id,可以查询多个品牌，设置多选，因此用List
     * brandId=1
     */
    private List<Long> brandId;

    /*
     * 按照属性进行筛选（品牌、价格、操作系统、屏幕尺寸）在这里都是属性
     * attrs=1_苹果    请求中的属性1 操作系统选择 苹果系统
     * attrs=2_5寸:6寸     请求中的属性2  屏幕尺寸  选择5寸和6寸
     * 在es中， SkuEsModel的attrs有内部类，我们这里的请求参数就不写内部类了，用split就可以切割出对应数据
     */
    private List<String> attrs;

    /*
     * 查询到的数据1页肯定装不下，因此还要分页，通过前端传来的pageNum决定怎么分页
     */
    private Integer pageNum = 1; //默认页码1 es中索引是从0开始，1只是方便阅读，但代码中要写成pageNum-1

    /*
     * URL末尾的`查询条件`  比如：http://xxxxxxxxx?&attrs=1_苹果
     */
    private String _queryString;  //一般元数据使用_开头。 只是查询条件 并非整个URL
}
