package com.taoji666.gulimall.order.feign;

import com.taoji666.common.to.SkuHasStockVo;
import com.taoji666.common.utils.R;
import com.taoji666.gulimall.order.vo.FareVo;
import com.taoji666.gulimall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/*
* 远程调用 库存系统
* 查询商品的库存信息，并返回
* */
@FeignClient("gulimall-ware")
public interface WareFeignService {

    @RequestMapping("ware/waresku/hasStockNR")
    List<SkuHasStockVo> getSkuHasStockNR(@RequestBody List<Long> ids);

    @RequestMapping("ware/wareinfo/fare/{addrId}")
    FareVo getFare(@PathVariable("addrId") Long addrId);

    @RequestMapping("ware/waresku/lock/order")
    R orderLockStock(@RequestBody WareSkuLockVo itemVos);
}
