package com.taoji666.gulimall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.taoji666.common.exception.BizCodeEnume;
import com.taoji666.common.exception.NoStockException;
import com.taoji666.gulimall.ware.entity.WareSkuEntity;
import com.taoji666.gulimall.ware.service.WareSkuService;
import com.taoji666.gulimall.ware.vo.SkuHasStockVo;
import com.taoji666.gulimall.ware.vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * 商品库存
 *
 * @author Taoji
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;


    /**
     * 查询sku是否有库存
     * 用于es，是es相关to SkuEsModel中的属性 Boolean hasStock
     *
     * @return
     */
    @PostMapping(value = "/hasStock")
    public R getSkuHasStock(@RequestBody List<Long> skuIds) {
        //新建一个SkuHasStockVo 只要skuId 和 是否有库存
        //编写getSkuHasStock方法来检查有没有库存
        List<SkuHasStockVo> vos = wareSkuService.getSkuHasStock(skuIds);

        //会返回一个崭新的R匿名对象,将查到的vos 暂时存入R的匿名对象，但是返回值有名字，就存入r了
        return R.ok().setData(vos);
    }
    //上面方法的孪生版，由订单微服务远程调用，由于不想fastjson转换，所以不用R，直接返回
    @PostMapping(value = "/hasStockNR")
    public List<SkuHasStockVo> getSkuHasStockNR(@RequestBody List<Long> skuIds) {
        //新建一个SkuHasStockVo 只要skuId 和 是否有库存
        //编写getSkuHasStock方法来检查有没有库存
        List<SkuHasStockVo> vos = wareSkuService.getSkuHasStock(skuIds);

        //不存入R了，直接用
        return vos;
    }

    /**
     *文档库存系统02、查询商品库存
     * GET  /ware/waresku/list
     * {
     *    page: 1,//当前页码
     *    limit: 10,//每页记录数
     *    sidx: 'id',//排序字段
     *    order: 'asc/desc',//排序方式
     *    wareId: 123,//仓库id
     *    skuId: 123//商品id
     * }
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * 订单微服务远程调用，，下订单时锁库存
     * @param lockVo
     * @return
     */
    @RequestMapping("/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo lockVo) { //@RequestBody将提交过来的json转换成对象
        try {
            Boolean lock = wareSkuService.orderLockStock(lockVo);
            return R.ok();
        } catch (NoStockException e) {
            return R.error(BizCodeEnume.NO_STOCK_EXCEPTION.getCode(), BizCodeEnume.NO_STOCK_EXCEPTION.getMsg());
        }
    }

}
