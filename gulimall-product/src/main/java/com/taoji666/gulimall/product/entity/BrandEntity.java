package com.taoji666.gulimall.product.entity;

import com.taoji666.common.valid.AddGroup;
import com.taoji666.common.valid.ListValue;
import com.taoji666.common.valid.UpdateGroup;
import com.taoji666.common.valid.UpdateStatusGroup;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;

/**
 * 品牌
 *
 * @author Taoji
 *
 * 这里做了服务端校验JSR303，见主程序处
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 品牌id
	 * 这里使用了分组校验
	 * 不同的场合 新增 或者 更新（groups） 使用的是不同的校验规则（@NotNull @Null 等） message是返回的错误信息
	 * groups里面的类 专门写到common子工程的valid包下，只用写一个接口就好，因为这里只需要一个类的名字。controller通过这个名字，找到这里来看要不要校验
	 * 选择使用哪个分组执行校验，在controller里面的@Validated中指定
	 *
	 * 一旦使用了分组注解，就必须添加groups属性，否则，默认都不校验
	 */
	@NotNull(message = "修改必须指定品牌id",groups = {UpdateGroup.class})
	@Null(message = "新增不能指定id",groups = {AddGroup.class})
	@TableId
	private Long brandId;
	/**
	 * 品牌名
	 * name属性，不管是是新增，还是更新都需要校验
	 */
	@NotBlank(message = "品牌名必须提交",groups = {AddGroup.class,UpdateGroup.class})
	private String name;
	/**
	 * 品牌logo地址
	 */
	@NotBlank(groups = {AddGroup.class})
	@URL(message = "logo必须是一个合法的url地址",groups={AddGroup.class,UpdateGroup.class})
	private String logo;
	/**
	 * 介绍
	 */
	private String descript;
	/**
	 * 显示状态[0-不显示；1-显示]
	 */
//	@Pattern()
	@NotNull(groups = {AddGroup.class, UpdateStatusGroup.class})
  	@ListValue(vals={0,1},groups = {AddGroup.class, UpdateStatusGroup.class}) //这个是自定义注解
	private Integer showStatus;
	/**
	 * 检索首字母
	 */
	@NotEmpty(groups={AddGroup.class})
	@Pattern(regexp="^[a-zA-Z]$",message = "检索首字母必须是一个字母",groups={AddGroup.class,UpdateGroup.class})
	private String firstLetter;
	/**
	 * 排序
	 */
	@NotNull(groups={AddGroup.class})
	@Min(value = 0,message = "排序必须大于等于0",groups={AddGroup.class,UpdateGroup.class})
	private Integer sort;

}
