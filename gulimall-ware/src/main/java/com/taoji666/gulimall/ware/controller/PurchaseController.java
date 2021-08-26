package com.taoji666.gulimall.ware.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.taoji666.gulimall.ware.vo.MergeVo;
import com.taoji666.gulimall.ware.vo.PurchaseDoneVo;
import com.taoji666.gulimall.ware.entity.PurchaseEntity;
import com.taoji666.gulimall.ware.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * 采购信息
 *
 * @author TaoJi
 */


@RestController
@RequestMapping("ware/purchase")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;

    /* 库存系统07 完成采购
    POST   /ware/purchase/done
    {
   id: 123,   //采购单id
   items: [{itemId:1,status:4,reason:""}]// 哪些项目已经完成采购，没有完成，原因是什么
}
    */
    @PostMapping("/done")
    public R finish(@RequestBody PurchaseDoneVo doneVo){

        purchaseService.done(doneVo);

        return R.ok();
    }

    /**
     * 领取采购单
     * POST   /ware/purchase/received
     *前端请求
     *[1,2,3,4]//前端传来的采购单id（即将领取的）
     */
    @PostMapping("/received")
    public R received(@RequestBody List<Long> ids){

        purchaseService.received(ids);

        return R.ok();
    }

    ///ware/purchase/unreceive/list
    /*
* POST  /ware/purchase/merge
*
*
* {
  purchaseId: 1, //整单id
  items:[1,2,3,4] //合并项集合
}
* 专门为这两个参数写一个 MergeVo 来收集前端数据
*  items:[1,2,3,4] //合并项集合  将采购详情单1、2、3、4 合并成一个采购单ID
*/
    @PostMapping("/merge")
    public R merge(@RequestBody MergeVo mergeVo){ //POST请求，用@RequestBody来转换json数据

        purchaseService.mergePurchase(mergeVo);


        return R.ok();
    }
/*

    库存系统05 查询未领取的采购单
    GET   /ware/purchase/unreceive/list
*/

    @RequestMapping("/unreceive/list")
    //@RequiresPermissions("ware:purchase:list")
    public R unreceivelist(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseService.queryPageUnreceivePurchase(params);

        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:purchase:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:purchase:info")
    public R info(@PathVariable("id") Long id){
		PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok().put("purchase", purchase);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:purchase:save")
    public R save(@RequestBody PurchaseEntity purchase){
        purchase.setUpdateTime(new Date());
        purchase.setCreateTime(new Date());
		purchaseService.save(purchase);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:purchase:update")
    public R update(@RequestBody PurchaseEntity purchase){
		purchaseService.updateById(purchase);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:purchase:delete")
    public R delete(@RequestBody Long[] ids){
		purchaseService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
