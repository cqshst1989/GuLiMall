package com.taoji666.gulimall.product.vo;

import com.taoji666.gulimall.product.entity.AttrEntity;
import lombok.Data;

import java.util.List;

@Data
public class AttrGroupWithAttrsVo {

    /**
     * 分组id
     */
    private Long attrGroupId;
    /**
     * 组名
     */
    private String attrGroupName;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 描述
     */
    private String descript;
    /**
     * 组图标
     */
    private String icon;
    /**
     * 所属分类id
     */
    private Long catelogId;

    //以上复制attrGroupEntity的所有，可以用继承
   //新增AttrEntity  因为，前端要求返回attrentity
    private List<AttrEntity> attrs;
}
