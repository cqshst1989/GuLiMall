package com.taoji666.gulimall.product.dao;

import com.taoji666.gulimall.product.entity.CategoryBrandRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 品牌分类关联
 * 
 * @author Taoji
 */
@Mapper
public interface CategoryBrandRelationDao extends BaseMapper<CategoryBrandRelationEntity> {

    //用继承的方式写这个Dao
    //mybatis插件可以让我们飞快的进入该dao对应的 sql xml 映射文件
    /*@Param是给形参取名字，免得映射文件(xml)识别参数不方便。 取了名字后，sql语句中可直接用#{}取出参数。
     潜规则：一旦有两个名字，都用@Param取名字 */
    void updateCategory(@Param("catId") Long catId, @Param("name") String name);

}
