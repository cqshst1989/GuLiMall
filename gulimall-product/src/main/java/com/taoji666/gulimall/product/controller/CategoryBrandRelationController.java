package com.taoji666.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.taoji666.gulimall.product.entity.BrandEntity;
import com.taoji666.gulimall.product.vo.BrandVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.taoji666.gulimall.product.entity.CategoryBrandRelationEntity;
import com.taoji666.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * 品牌分类关联
 *
 * @author TaoJi
 */
@RestController
@RequestMapping("product/categorybrandrelation")
public class CategoryBrandRelationController {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    /**
     * 根据请求体中的brandId获取当前品牌关联的所有分类列表
     * URL 和 返回参数  都要根据 谷粒商城接口文档（15、获取品牌关联的分类）来写
     */
    @GetMapping("/catelog/list") //@RequestMapping(Method=RequestMethod.GET)的简写版
    //@RequiresPermissions("product:categorybrandrelation:list")
    public R cateloglist(@RequestParam("brandId")Long brandId){
        List<CategoryBrandRelationEntity> data = categoryBrandRelationService.list(
                new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id",brandId)
        );

        return R.ok().put("data", data);
    }

    /**
     *  商品系统 14 /product/categorybrandrelation/brands/list
     *  获取分类关联的品牌.
     *  窗口选择分类catid比如手机，自动回显出 华为，小米，oppo等
     *
     *  由于只要求回显  id 和 品牌名， 因此创建brandvo
     *
     *  1、Controller：处理请求，接受和校验数据
     *  2、Service接受controller传来的数据，进行业务处理
     *  3、Controller接受Service处理完的数据，封装页面指定的vo
     */
    @GetMapping("/brands/list")
    //required=true 这个参数必须存在
    public R relationBrandsList(@RequestParam(value = "catId",required = true)Long catId){

        //通过分类id 去 品牌分类关系表中 查到当前分类（手机）对应的所有品牌
        //关系表的方法，返回的是 属性表实体。所以逆向工程没法生成，自己写方法
        List<BrandEntity> vos = categoryBrandRelationService.getBrandsByCatId(catId);

        //我们只要id 和 name 。其他属性都不要，通过stream方法，过滤出 id 和 name 赋值给 vo
        List<BrandVo> collect = vos.stream().map(item -> {
            BrandVo brandVo = new BrandVo();
            brandVo.setBrandId(item.getBrandId());
            brandVo.setBrandName(item.getName());

            return brandVo;
        }).collect(Collectors.toList());

        return R.ok().put("data",collect);

    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:categorybrandrelation:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = categoryBrandRelationService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("product:categorybrandrelation:info")
    public R info(@PathVariable("id") Long id){
		CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok().put("categoryBrandRelation", categoryBrandRelation);
    }

    /**
     * 保存
     * save方法查询的是CategoryBrandRelationEntity 表，这个表冗余设计了brand_name 和 catelog_name
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:categorybrandrelation:save")
    public R save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){


		categoryBrandRelationService.saveDetail(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:categorybrandrelation:update")
    public R update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.updateById(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:categorybrandrelation:delete")
    public R delete(@RequestBody Long[] ids){
		categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
