package com.taoji666.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.taoji666.gulimall.product.entity.ProductAttrValueEntity;
import com.taoji666.gulimall.product.service.ProductAttrValueService;
import com.taoji666.gulimall.product.vo.AttrRespVo;
import com.taoji666.gulimall.product.vo.AttrVo;
import com.taoji666.gulimall.product.service.AttrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * 商品属性
 *
 * @author TaoJi
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    /*GET  /product/attr/info/{attrId}
       商品系统19 获取spu规格    （用于修改spu的时候回显）
       要求返回的 刚好就是实体类本身，不需要vo
    */
    @GetMapping("/base/listforspu/{spuId}")
    public R baseAttrlistforspu(@PathVariable("spuId") Long spuId){

        List<ProductAttrValueEntity> entities = productAttrValueService.baseAttrlistforspu(spuId);

        return R.ok().put("data",entities);
    }
    //查询商品属性，全查或者 按照 模糊查询。查询出结果后，分页回显
    //根据接口文档05 获取分类规格参数product/attr/sale/list/0?
    //product/attr/base/list/{catelogId}
    @GetMapping("/{attrType}/list/{catelogId}")
    public R baseAttrList(@RequestParam Map<String, Object> params,
                          @PathVariable("catelogId") Long catelogId,//三级分类的id
                          @PathVariable("attrType")String type){ //是否允许被索引

        PageUtils page = attrService.queryBaseAttrPage(params,catelogId,type);
        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:attr:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 前端修改商品属性的时候，需要回显的信息
     * 对应谷粒商城接口的 07 查询属性详情
     * 这里继续使用attrvo
     */
    @RequestMapping("/info/{attrId}")
    //@RequiresPermissions("product:attr:info")
    public R info(@PathVariable("attrId") Long attrId){
		//AttrEntity attr = attrService.getById(attrId);学了vo以后，就不用这个了
        AttrRespVo respVo = attrService.getAttrInfo(attrId);//新加了属性的vo，没办法用generator生成方法，只能自己写

        return R.ok().put("attr", respVo);
    }

    /**
     * 保存
     *  来传数据。从前端获取的东西，先给AttrVo，不用直接给PO了（entity）
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attr:save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr); //这个方法又得自己编写

        return R.ok();
    }

    ///product/attrgroup/attr/relation/delete


    /**
     * 修改
     * 凡是使用vo的 都得自己重写实现类，但非常简单，就是组装各个不同数据表实体的对应属性
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attr:update")
    public R update(@RequestBody AttrVo attr){
		attrService.updateAttr(attr);

        return R.ok();
    }

    /* POST  /product/attr/update/{spuId}
        商品系统 23、修改商品规格
        请求参数：  （要修改的地方）
        [{
    "attrId": 7,
    "attrName": "入网型号",
    "attrValue": "LIO-AL00",
    "quickShow": 1
}, {
    "attrId": 14,
    "attrName": "机身材质工艺",
    "attrValue": "玻璃",
    "quickShow": 0
}, {
    "attrId": 16,
    "attrName": "CPU型号",
    "attrValue": "HUAWEI Kirin 980",
    "quickShow": 1
}]
由于修改非常麻烦，最好的办法是，直接删除原来的数据（通过路径变量），然后插入新的数据（通过ProductAttrValueEntity）
     */
    @PostMapping("/update/{spuId}")
    public R updateSpuAttr(@PathVariable("spuId") Long spuId,
                           @RequestBody List<ProductAttrValueEntity> entities){

        productAttrValueService.updateSpuAttr(spuId,entities);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attr:delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
