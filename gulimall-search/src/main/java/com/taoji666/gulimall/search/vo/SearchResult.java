package com.taoji666.gulimall.search.vo;


import com.taoji666.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: TaoJi
 * @date: 2021/8/9 14:22
 *
 * 根据需求从数据库中查出经常要查的数据，封装进SkuEsModel，再放进ES中。
 * 现在从ES中，根据查询ES查询条件（构造的DSL查询语句）查出的结果，同样也是封装进SkuEsModel
 * 最后从SkuEsModel 取出想要的数据，放进SearchResult
 *
 * 后面的分页，是直接从请求参数中得到的数据
 * 后面的聚合，都是根据分析回传的SkuEsModel，得到的数据
 *
 * 区别：存进ES的 SkuEsModel
 * skuEsModel是一个SKU ，属性都是这个SKU的属性，属性之间都有相互关系
 * SearchResult 完全就是封装要返回给前端的种种数据，属性都是没关系的  前端要的各种需求。 有关系的属性，就写内部类
 */
@Data
public class SearchResult {
    /**
     * 从ES中查询到的所有商品信息
     * SkuEsModel已经在ElasticSaveController中被加入ES
     * 查出来的数据依然返回的是SkuEsModel
     */
    private List<SkuEsModel> product;  //product就是一个SkuEsModel

    /**
     * 以下是通过分析返回的SkuEsModel 对象 product，聚合里面的某些属性
     * */
    private List<BrandVo> brands; //聚合 当前查询到的结果锁涉及到多少种品牌，单独列出来
    private List<AttrVo> attrs;//聚合  当前查询到的结果，所有涉及到的所有属性（比如屏幕尺寸，操作系统）
    private List<CatalogVo> catalogs;//聚合 当前查询到的结果，所有涉及到的所有分类。华为可能是手机分类，也可能是路由器分类

    /**
     * 以下是分页信息，是页面请求参数决定的。 最终显示在前端，就是商品一共有x页，现在是第y页
     */
    private Integer pageNum;//当前页码
    private Long total; //总记录数
    private Integer totalPages; //总页码

    private List<Integer> pageNavs; //可遍历的页码，就是淘宝右下角的第 1、2、3、4、5、6....直到totalPages页

    /* 面包屑导航数据   就是已经选定的  筛选条件(属性) 展示，并且点击X可以删除筛选条件，
    * 比如： 操作系统：IOS X   品牌：苹果 X
    *
    * 一旦选中了一个属性（筛选条件），这个属性就会变成面包屑。同时会根据该面包屑，再生成新的商品属性，和商品数据
    * 具体上淘宝试试就知道了
    * */
    private List<NavVo> navs = new ArrayList<>(); //给默认值，防止navs为空，出现异常

    //===========================以上是返回给页面的所有信息，下面是和上面聚合的内部类============================//




    @Data
    public static class NavVo {
        private String navName;
        private String navValue;
        private String link;  //点击X，去掉筛选后，要跳转去的请求服务的URL
    }


    @Data
    public static class BrandVo {

        private Long brandId;

        private String brandName;

        private String brandImg;
    }


    @Data
    public static class AttrVo { //用于在前端头部展示，便于二次筛选

        private Long attrId;

        private String attrName;  //比如屏幕尺寸，操作系统

        private List<String> attrValue; //每个属性可能对应的值4.6寸 7寸，IOS，Android等等
    }


    @Data
    public static class CatalogVo {

        private Long catalogId;

        private String catalogName;
    }
}
