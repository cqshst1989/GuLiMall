package com.taoji666.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;



/**
 * 商品三级分类
 * 
 * @author Taoji
 */

@Data
@TableName("pms_category")
public class CategoryEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 分类id
	 */
	@TableId
	private Long catId;
	/**
	 * 分类名称
	 */
	private String name;
	/**
	 * 父分类id
	 */
	private Long parentCid;
	/**
	 * 层级
	 */
	private Integer catLevel;
	/**
	 *逻辑删除字段 是否显示[0-不显示，1显示]
	 * 逻辑删除本质就是用 update语句将数据库里的逻辑删除字段改成0
	 * 同时，在查询的时候，这个字段为0的，也不查了
	 */
	@TableLogic(value = "1",delval = "0")
	private Integer showStatus;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 图标地址
	 */
	private String icon;
	/**
	 * 计量单位
	 */
	private String productUnit;
	/**
	 * 商品数量
	 */
	private Integer productCount;


	//children在一级一级的递归找下去，到最后一级总会没children了，会返回一个空数组，前端不好处理这个空数组
	//@JsonInclude 可以让children不为空才返回给前端，为空就不返回了
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@TableField(exist=false)
	private List<CategoryEntity> children;



}
