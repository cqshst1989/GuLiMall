package com.taoji666.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.to.mq.OrderTo;
import com.taoji666.common.to.mq.StockLockedTo;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.ware.entity.WareSkuEntity;
import com.taoji666.gulimall.ware.vo.SkuHasStockVo;
import com.taoji666.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author Taoji
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    //为某个订单锁定库存
    Boolean orderLockStock(WareSkuLockVo lockVo);

    void unlock(StockLockedTo stockLockedTo);

    void unlock(OrderTo orderTo);
}

