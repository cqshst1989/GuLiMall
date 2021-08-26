package com.taoji666.gulimall.product.service.impl;

import com.taoji666.gulimall.product.dao.BrandDao;
import com.taoji666.gulimall.product.dao.CategoryDao;
import com.taoji666.gulimall.product.entity.BrandEntity;
import com.taoji666.gulimall.product.entity.CategoryEntity;
import com.taoji666.gulimall.product.service.BrandService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.taoji666.gulimall.product.dao.CategoryBrandRelationDao;
import com.taoji666.gulimall.product.entity.CategoryBrandRelationEntity;
import com.taoji666.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    BrandDao brandDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationDao relationDao;

    @Autowired
    BrandService brandService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        //将CategoryBrandRelationEntity 中的 brandId 和  catelogId 读取出来
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();
        //1、查询详细名字
        BrandEntity brandEntity = brandDao.selectById(brandId);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);

        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setCatelogName(categoryEntity.getName());

        this.save(categoryBrandRelation); //也可以用自己的dao来save

    }

    @Override
    public void updateBrand(Long brandId, String name) {
        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
        relationEntity.setBrandId(brandId);
        relationEntity.setBrandName(name);
        //update方法里的第二个参数是更新条件wrapper: 更新数据表里的第几行数据 brand_id字段决定要更新的行
        this.update(relationEntity,new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id",brandId));
    }
    //方法mybatis已经提供了，但默认方法没有按照id 来修改 name的，所以只能自行重写
    //接着去对应的dao，写该方法的接口
    //很多次了，本service中，不需要通过注入自家dao来调取相应的crud方法
    @Override
    public void updateCategory(Long catId, String name) {
        this.baseMapper.updateCategory(catId,name);
    }

    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {
        List<CategoryBrandRelationEntity> catelogId = relationDao.selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));
        List<BrandEntity> collect = catelogId.stream().map(item -> {
            Long brandId = item.getBrandId(); //把brandId字段取出来
            BrandEntity byId = brandService.getById(brandId); //通过brandId字段查到BrandEntity属性表
            return byId; //返回属性表
        }).collect(Collectors.toList());
        return collect;
    }

}