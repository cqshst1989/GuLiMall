package com.taoji666.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.taoji666.gulimall.product.entity.AttrEntity;
import com.taoji666.gulimall.product.service.AttrAttrgroupRelationService;
import com.taoji666.gulimall.product.service.AttrService;
import com.taoji666.gulimall.product.service.CategoryService;
import com.taoji666.gulimall.product.vo.AttrGroupRelationVo;
import com.taoji666.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.taoji666.gulimall.product.entity.AttrGroupEntity;
import com.taoji666.gulimall.product.service.AttrGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * 属性分组
 *
 * @author Taoji
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService; //注入这个来查询商品的tree路径

    @Autowired
    AttrService attrService;

    @Autowired
    AttrAttrgroupRelationService relationService;

    //参考谷粒商城接口文档11 /product/attrgroup/attr/relation
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> vos){

        relationService.saveBatch(vos);
        return R.ok();
    }

    //商品系统17    /product/attrgroup/{catelogId}/withattr
    //获取分类下所有分组&关联属性
    //点击发布商品后，回显 分组 和 关联属性 的相关内容


    @GetMapping("/{catelogId}/withattr")
    public R getAttrGroupWithAttrs(@PathVariable("catelogId")Long catelogId){

        //1、查出当前分类下的所有属性分组，
        //2、查出每个属性分组的所有属性
        //由于要新增很多内容，因此还是用vo来做
       List<AttrGroupWithAttrsVo> vos =  attrGroupService.getAttrGroupWithAttrsByCatelogId(catelogId);
       return R.ok().put("data",vos);
    }



    //根据谷粒商城接口文档 10 product/attrgroup/attr/relation
    //编写属性 和 所有分组的关联：查询分组，获取属性列表 具体看文档的请求和响应数据
    @GetMapping("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId){
        List<AttrEntity> entities =  attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data",entities);
    }

    //谷粒商城文档13、获取属性分组没有关联的其他属性
    // /product/attrgroup/{attrgroupId}/noattr/relation
    // attrgroupid这个分组，除了已经关联的，还有哪些没关联，找到了，可以看心情再关联一点属性
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R attrNoRelation(@PathVariable("attrgroupId") Long attrgroupId,
                            @RequestParam Map<String, Object> params){
        PageUtils page = attrService.getNoRelationAttr(params,attrgroupId);
        return R.ok().put("page",page);
    }
    //响应product/attrgroup/attr/relation/delete 的请求
    //谷粒商城文档12、删除属性与分组的关联关系
    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody  AttrGroupRelationVo[] vos){ //前端是post请求，在请求体里面 @RequestBody将请求体中的数据转成成json。get请求不用@Requestmapping，因为没有请求体
        attrService.deleteRelation(vos);
        return R.ok();
    }

    /**
     * 列表
     * @RequestParam取出 url中 ?x=y 的值
     * @PathVariable 直接取出url中的路径值/{xxx}
     *
     * 这里是按照 谷粒商城开发文档 公共模型下的 分页返回数据要求 来写得代码，主要是匹配需求的返回值
     *去 AttrGroupServiceImpl 中寻找queryPage(params,catelogId) 方法
     */
    @RequestMapping("/list/{catelogId}")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params,
                  @PathVariable("catelogId") Long catelogId){
//        PageUtils page = attrGroupService.queryPage(params);

        PageUtils page = attrGroupService.queryPage(params,catelogId); //自己编写的分页查询结果，returan回来

        return R.ok().put("page", page); //将分页查询结果 返回前端
    }


    /**
     * 信息
     * 补充商品的tree路径，路径也需要回显给前端
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId); //attrgroup数据表

        Long catelogId = attrGroup.getCatelogId(); //attrgroup数据表的所属分类ID
        Long[] path = categoryService.findCatelogPath(catelogId);//自行编写数据找到商品tree路径的方法，找到该所属分类ID的tree

        attrGroup.setCatelogPath(path);

        return R.ok().put("attrGroup", attrGroup);
    }



    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
