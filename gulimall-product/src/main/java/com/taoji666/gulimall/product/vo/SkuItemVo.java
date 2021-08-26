package com.taoji666.gulimall.product.vo;


import com.taoji666.gulimall.product.entity.SkuImagesEntity;
import com.taoji666.gulimall.product.entity.SkuInfoEntity;
import com.taoji666.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author Taoji
 * @createTime: 2021-08-16 10:50
 **/
@ToString
@Data
public class SkuItemVo {

    //1、sku基本信息的获取  直接拿pms_sku_info对应的实体类
    private SkuInfoEntity info;

    private boolean hasStock = true;//商品是否有货，默认有货

    //2、sku的图片信息    查pms_sku_images，有多张图片，所以是List
    private List<SkuImagesEntity> images;

    //3、获取spu的销售属性组合  比如 银色，8+128G
    private List<SkuItemSaleAttrVo> saleAttr;

    //4、获取spu的介绍  查pms_category数据表  其实是商品介绍图片
    private SpuInfoDescEntity desc;

    //5、获取spu的规格参数信息  比如整个iPhone13 的规格参数信息
    private List<SpuItemAttrGroupVo> groupAttrs;

    //6、秒杀商品的优惠信息
    private SeckillSkuVo seckillSkuVo;

    //特别注意：mybatis的Mapper.xml 中SQL语句不识别内部类，所以内部类也单独写在vo里面

}