package com.taoji666.gulimall.product.service;

import com.taoji666.gulimall.product.vo.SpuSaveVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.product.entity.SpuInfoEntity;

import java.util.Map;

/**
 * spu信息
 *
 * @author TaoJi
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo vo);

    void saveBaseSpuInfo(SpuInfoEntity infoEntity);


    PageUtils queryPageByCondition(Map<String, Object> params);

    //商品上架
    void up(Long spuId);

    //订单微服务远程调用，通过skuId查询spu相关属性并设置
    SpuInfoEntity getSpuBySkuId(Long skuId);
}

