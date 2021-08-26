package com.taoji666.gulimall.product.dao;

import com.taoji666.gulimall.product.entity.AttrGroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taoji666.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 * 
 * @author Taoji
 * @date 2021-08-16 16:44:51
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {

    //入参有2个以上，就用@Param取名字，这样xml文件中，就可以直接用这个名字了
    List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(@Param("spuId") Long spuId, @Param("catalogId") Long catalogId);
}
