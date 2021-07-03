package com.taoji666.gulimall.gulimallproduct.dao;

import com.taoji666.gulimall.gulimallproduct.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-03 20:07:22
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
