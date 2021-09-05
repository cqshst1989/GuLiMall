package com.taoji666.gulimall.product.controller;

import java.util.Arrays;
import java.util.Map;

import com.taoji666.gulimall.product.vo.SpuSaveVo;
import com.taoji666.gulimall.product.entity.SpuInfoEntity;
import com.taoji666.gulimall.product.service.SpuInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * spu信息
 *
 * @author Taoji
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;

    /**
     * 18、spu检索
     *
     * 前端请求参数： GET   /product/spuinfo/list
     *{
     *    page: 1,//当前页码
     *    limit: 10,//每页记录数
     *    sidx: 'id',//排序字段
     *    order: 'asc/desc',//排序方式
     *    key: '华为',//检索关键字
     *    catelogId: 6,//三级分类id
     *    brandId: 1,//品牌id
     *    status: 0,//商品状态
     * }
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:spuinfo:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = spuInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("product:spuinfo:info")
    public R info(@PathVariable("id") Long id){
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 保存
     *对应的是商品系统19新增商品 /product/spuinfo/save
     * 根据文档，要新增非常非常多的东西啊
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:spuinfo:save")

    public R save(@RequestBody SpuSaveVo vo){
		//spuInfoService.save(spuInfo);

        spuInfoService.saveSpuInfo(vo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:spuinfo:update")
    public R update(@RequestBody SpuInfoEntity spuInfo){
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:spuinfo:delete")
    public R delete(@RequestBody Long[] ids){
		spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * 商品上架功能
     *商品系统 POST /product/spuinfo/{spuId}/up
     */
    @PostMapping("/{spuId}/up")
    public R upSpu(@PathVariable Long spuId) {
        spuInfoService.up(spuId);
        return R.ok();
    }

    //订单微服务远程调用，通过skuId查询spu相关属性并设置
    @RequestMapping("/skuId/{skuId}")
    public R getSpuBySkuId(@PathVariable("skuId") Long skuId) {
        SpuInfoEntity spuInfoEntity = spuInfoService.getSpuBySkuId(skuId);
        return R.ok().setData(spuInfoEntity);
    }




}
