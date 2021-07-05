package com.taoji666.gulimall.gulimallorder.controller;

import java.util.Arrays;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taoji666.gulimall.gulimallorder.entity.OrderReturnReasonEntity;
import com.taoji666.gulimall.gulimallorder.service.OrderReturnReasonService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * 退货原因
 *
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-04 11:43:11
 */
@RestController
@RequestMapping("gulimallorder/orderreturnreason")
public class OrderReturnReasonController {
    @Autowired
    private OrderReturnReasonService orderReturnReasonService;

    /**
     * 列表
     */
    @RequestMapping("/list")
   // @RequiresPermissions("gulimallorder:orderreturnreason:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = orderReturnReasonService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("gulimallorder:orderreturnreason:info")
    public R info(@PathVariable("id") Long id){
		OrderReturnReasonEntity orderReturnReason = orderReturnReasonService.getById(id);

        return R.ok().put("orderReturnReason", orderReturnReason);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
   // @RequiresPermissions("gulimallorder:orderreturnreason:save")
    public R save(@RequestBody OrderReturnReasonEntity orderReturnReason){
		orderReturnReasonService.save(orderReturnReason);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
   // @RequiresPermissions("gulimallorder:orderreturnreason:update")
    public R update(@RequestBody OrderReturnReasonEntity orderReturnReason){
		orderReturnReasonService.updateById(orderReturnReason);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
   // @RequiresPermissions("gulimallorder:orderreturnreason:delete")
    public R delete(@RequestBody Long[] ids){
		orderReturnReasonService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
