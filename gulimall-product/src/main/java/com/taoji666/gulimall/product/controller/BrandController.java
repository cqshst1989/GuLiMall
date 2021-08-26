package com.taoji666.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.taoji666.common.valid.AddGroup;
import com.taoji666.common.valid.UpdateGroup;
import com.taoji666.common.valid.UpdateStatusGroup;
import com.taoji666.gulimall.product.entity.BrandEntity;
import com.taoji666.gulimall.product.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;


/**
 * 品牌
 *
 * @author Taoji
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:brand:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    //@RequiresPermissions("product:brand:info")
    public R info(@PathVariable("brandId") Long brandId){
		BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }
    /**
     * 查询该关键字对应分类下的所有品牌
     */
    @RequestMapping("/infos")
    public R info(@RequestParam("brandIds")List<Long> brandIds){
        List<BrandEntity> brand = brandService.getBrandsByIds(brandIds);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     * @Validated 选择，校验哪个分组 （校验分组在BrandEntity实体类里面就指定好啦）
     * 这里是校验新增
     * 没有写groups的注解，在分组校验的情况下（controller中制定了@validated'），默认都不生效
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")  BindingResult是附带的验证结果，如果不写BindingResult，遇到异常就会抛出
    //我们后面将抛出的异常集中在exception包下处理
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand/*,BindingResult result*/){
//        if(result.hasErrors()){
//            Map<String,String> map = new HashMap<>();
//            //1、获取校验的错误结果
//            result.getFieldErrors().forEach((item)->{
//                //FieldError 获取到错误提示
//                String message = item.getDefaultMessage();
//                //获取错误的属性的名字
//                String field = item.getField();
//                map.put(field,message);
//            });
//
//            return R.error(400,"提交的数据不合法").put("data",map);
//        }else {
//
//        }

        brandService.save(brand);


        return R.ok();
    }

    /**
     * 修改
     * 由于存在冗余设计，更新这个字段的时候，其他冗余的相同字段也得更新
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:brand:update")
    public R update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand){
		brandService.updateDetail(brand);

        return R.ok();
    }
    /**
     * 这里新增了一个只修改状态，因为前端可以通过开关来只改变状态
     * 修改状态
     */
    @RequestMapping("/update/status")
    //@RequiresPermissions("product:brand:update")
    public R updateStatus(@Validated(UpdateStatusGroup.class) @RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:brand:delete")
    public R delete(@RequestBody Long[] brandIds){
		brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
