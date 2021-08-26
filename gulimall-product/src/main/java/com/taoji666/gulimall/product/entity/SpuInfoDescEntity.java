package com.taoji666.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * spu信息介绍
 * @author Taoji
 */
@Data
@TableName("pms_spu_info_desc")
public class SpuInfoDescEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 商品id
	 */
	@TableId(type = IdType.INPUT) //告诉mybatis这是非自增主键，否则mybatis看到主键就以为是自增，就不会读取前端传来的id
	private Long spuId;
	/**
	 * 商品介绍图片地址。用'，'分割了的多个商品描述图片
	 */
	private String decript;

}
