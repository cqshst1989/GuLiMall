package com.taoji666.gulimall.product.dao;

import com.taoji666.gulimall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 * 
 * @author TaoJi
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {
//    UPDATE `pms_spu_info` SET publish_status = #{code}, update_time = NOW() WHERE id = #{spuId}
    void updateSpuStatus(@Param("spuId") Long spuId, @Param("code") int code);
}
