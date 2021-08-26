package com.taoji666.gulimall.product.web;

import com.taoji666.gulimall.product.service.SkuInfoService;
import com.taoji666.gulimall.product.vo.SkuItemVo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;

/**
 * @author Taoji
 * @createTime: 2021-08-16 10:12
 *
 * SKU详情页（商品详情页）
 *
 **/
@Controller
public class ItemController {

    @Resource
    private SkuInfoService skuInfoService;

    /**
     * 展示当前sku的详情
     *
     * @param skuId
     * 在list.html中，点击某个商品，通过链接来到这里。
     *  <a th:href="|http://item.gulimall.com/${product.skuId}.html|">
     */
    //路径变量，通过skuId确认展现的是哪个具体商品@GetMapping("/{skuId}.html")
    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model) throws ExecutionException, InterruptedException {
        System.out.println("准备查询" + skuId + "详情");
        //由于一个商品有那么多信息，因此需要将这些信息封装成一个vo（具体详情，随便淘宝，京东，点开一个就知道了）
        //为了提高效率，使用异步的方式来查询
        SkuItemVo vos = skuInfoService.item(skuId);
        model.addAttribute("item", vos);
        return "item";
    }
}

