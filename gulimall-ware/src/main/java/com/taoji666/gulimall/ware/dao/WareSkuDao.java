package com.taoji666.gulimall.ware.dao;

import com.taoji666.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author Taoji
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);


    Long getSkuStock(Long skuId);

    /*查询商品在哪些仓库有货*/
    List<Long> listWareIdsHasStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    Long lockWareSku(@Param("skuId") Long skuId, @Param("num") Integer num, @Param("wareId") Long wareId)
}
