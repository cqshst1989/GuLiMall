package com.taoji666.gulimall.product.vo;


import lombok.Data;

/* VO(value object)值对象
  通常用干业务层之闾的数据传递，和PO一样也是仅仅包含数据而已。
  但应是抽象出的业务对象，可以和表对应，也可以不，这根据业务的需要。
    用new关韃字创建，由GC回收的

    简单的说，作用就是
    1、封装请求；
    2、响应数据。
    使用场景：实际项目中前端用户界面显示的数据，和mysql中的数据字段总会有不一样的地方。可以在VO里面任意自增新属性。

    一般情况，将实体类（PO）的对象复制给他，然后再加点自己业务需要的新属性，但是mybatis相关的注解，都可以不要了

    有了vo，controller里面的入参，就可以传vo对象了。而且也不需要在Entity（po）的属性里面加无关属性标注@TableField(exist = false)
*/
@Data
public class AttrVo {
    /**
     * 属性id
     */
    private Long attrId;
    /**
     * 属性名
     */
    private String attrName;
    /**
     * 是否需要检索[0-不需要，1-需要]
     */
    private Integer searchType;
    /**
     * 值类型[0-为单个值，1-可以选择多个值]
     */
    private Integer valueType;
    /**
     * 属性图标
     */
    private String icon;
    /**
     * 可选值列表[用逗号分隔]
     */
    private String valueSelect;
    /**
     * 属性类型[0-销售属性，1-基本属性，2-既是销售属性又是基本属性]
     */
    private Integer attrType;
    /**
     * 启用状态[0 - 禁用，1 - 启用]
     */
    private Long enable;
    /**
     * 所属分类
     */
    private Long catelogId;
    /**
     * 快速展示【是否展示在介绍上；0-否 1-是】，在sku中仍然可以调整
     */
    private Integer showDesc;

    private Long attrGroupId;
}
