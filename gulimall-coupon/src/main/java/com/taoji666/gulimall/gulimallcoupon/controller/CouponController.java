package com.taoji666.gulimall.gulimallcoupon.controller;

import java.util.Arrays;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taoji666.gulimall.gulimallcoupon.entity.CouponEntity;
import com.taoji666.gulimall.gulimallcoupon.service.CouponService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * 优惠券信息
 *
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-04 11:15:33
 */
@RestController
@RequestMapping("gulimallcoupon/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;


    //测试让member来远程调用这个功能，自己代码。接口在member的feign包中
    @RequestMapping("/member/list")
    public R membercoupons(){    //全系统的所有返回都返回R
        // 应该去数据库查用户对于的优惠券，但这个我们简化了，不去数据库查了，构造了一个优惠券给他返回
        CouponEntity couponEntity = new CouponEntity();
        couponEntity.setCouponName("满100减10");//优惠券的名字
        //将查出来的值，封装在coupon键里放入 R某对象域。 调用者会有R实体对象接这个hashmap
        return R.ok().put("coupons",Arrays.asList(couponEntity));
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
   // @RequiresPermissions("gulimallcoupon:coupon:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = couponService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("gulimallcoupon:coupon:info")
    public R info(@PathVariable("id") Long id){
		CouponEntity coupon = couponService.getById(id);

        return R.ok().put("coupon", coupon);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
   // @RequiresPermissions("gulimallcoupon:coupon:save")
    public R save(@RequestBody CouponEntity coupon){
		couponService.save(coupon);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
   // @RequiresPermissions("gulimallcoupon:coupon:update")
    public R update(@RequestBody CouponEntity coupon){
		couponService.updateById(coupon);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
   // @RequiresPermissions("gulimallcoupon:coupon:delete")
    public R delete(@RequestBody Long[] ids){
		couponService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
