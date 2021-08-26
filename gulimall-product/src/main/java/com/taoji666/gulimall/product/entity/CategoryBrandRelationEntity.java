package com.taoji666.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 品牌分类关联
 * 
 * @author Taoji
 */
@Data
@TableName("pms_category_brand_relation")
public class CategoryBrandRelationEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 这个表用了冗余设计，主要由于电商查询量确实太大。不冗余，关联表格会被多次查询，影响性能
	 */
	@TableId
	private Long id;
	/**
	 * 品牌id
	 */
	private Long brandId;
	/**
	 * 分类id
	 */
	private Long catelogId;
	/**
	 * 
	 */
	private String brandName;
	/**
	 * 这里是冗余设计，因为前面brandId所在的brandEntity表里面就有brandName
	 */
	private String catelogName;
	/**
	 * 这里是冗余设计，因为前面catelogId所在的CategoryEntity表里面就有categoryName
	 */

}
