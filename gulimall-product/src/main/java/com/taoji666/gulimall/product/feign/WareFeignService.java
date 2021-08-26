package com.taoji666.gulimall.product.feign;


import com.taoji666.common.utils.R;
import com.taoji666.common.to.SkuHasStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-ware") //工程路径下 就不需要/
public interface WareFeignService {

    /**
     * 自己封装解析结果（或者改R类，加入泛型）
     * @param skuIds
     * @return
     */

    @PostMapping("/ware/waresku/hasStock")  //这里不是工程，是接着gulimall-ware 所以需要/
    R getSkuHasStock(@RequestBody List<Long> skuIds);

}
