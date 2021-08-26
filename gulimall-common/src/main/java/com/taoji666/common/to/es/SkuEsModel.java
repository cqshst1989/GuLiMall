package com.taoji666.common.to.es;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 传输对象，存储到es的数据
 * 微服务之间传输 使用的 实体对象 TO，由于两个微服务同时会使用这个对象，因此TO一般放在公共类common中
 *
 * @author: Taoji
 *
 * 全是按照需要检索的文档写对应的to
 */
@Data
public class SkuEsModel {

    private Long skuId;  //pms_sku_info数据表

    private Long spuId;  //pms_sku_info数据表

    private String skuTitle;  //pms_sku_info数据表  设置高亮后，会变成<b style='color:red'>前面查到的skuTitle的值</b>

    private BigDecimal skuPrice;  //pms_sku_info数据表

    private String skuImg;  //pms_sku_info数据表 存放的是图片地址，在阿里云上

    private Long saleCount;  //pms_sku_info数据表

    /**
     * 是否有库存
     */
    private Boolean hasStock; //wms_ware_sku

    /**
     * 热度
     */
    private Long hotScore;

    private Long brandId; //pms_brand数据表  //pms_sku_info数据表

    private String brandName; //pms_brand数据表

    private String brandImg; //pms_brand 数据表中的logo

    private Long catalogId; //pms_category 数据表的 cat_id

    private String catalogName; //pms_category 数据表的 name


    /*pms_attr数据表，通过内部类集合该数据表的所有字段。主要因为商品都是多个属性，比如手机有，屏幕尺寸，操作系统等多个属性
    因此是List，所以只能用内部类*/

    private List<Attrs> attrs;
    //pms_attr 数据表
    @Data
    public static class Attrs {

        private Long attrId;

        private String attrName;

        private String attrValue;
    }
}