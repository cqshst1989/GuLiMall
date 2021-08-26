package com.taoji666.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;


//SkuItemVo的内部类
/**
 * @author Taoji
 * @createTime: 2021-08-16 11:21
 *
 *
 * 该VO准备封装商品详情页面，
 */

 /*
   页面中的销售属性组合

  * attr_id  attr_name  attr_value  sku_ids
  * 9        颜色        白色          12,13,14
  * 9        颜色        紫色          24,25,26
  * 9        颜色        红色          21,22,23
  * 12       版本        128G         9,12,15,18,21,24
  * 12       版本        256G         10,13,16,19,22,25
  *
  具体确定是哪个sku，由于前端要点击颜色 和 版本 按钮，因此就前端用js来做吧
  * */
@Data
@ToString
public class SkuItemSaleAttrVo {

    private Long attrId;

    private String attrName;

    private List<AttrValueWithSkuIdVO> attrValues;

}

