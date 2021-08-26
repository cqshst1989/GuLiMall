package com.taoji666.gulimall.product.vo;

import lombok.Data;

@Data
public class AttrRespVo extends AttrVo {
    /**
     * 响应的东西又比entity中的东西多，因此要用vo。这次直接用继承
     * 			"catelogName": "手机/数码/手机", //所属分类名字
     * 			"groupName": "主体", //所属分组名字
     */
    private String catelogName;
    private String groupName;

    private Long[] catelogPath;
}
