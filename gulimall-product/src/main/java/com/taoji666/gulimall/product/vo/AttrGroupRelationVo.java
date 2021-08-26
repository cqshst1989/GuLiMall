package com.taoji666.gulimall.product.vo;

import lombok.Data;

//对应谷粒商城参考文档 12 删除属性与分组的关联关系
//就是单纯接收前端数据。不用继承实体，因为属性比实体少

@Data
public class AttrGroupRelationVo {

    //页面提交上来的是"attrId":1,"attrGroupId":2
    private Long attrId;
    private Long attrGroupId;
}
