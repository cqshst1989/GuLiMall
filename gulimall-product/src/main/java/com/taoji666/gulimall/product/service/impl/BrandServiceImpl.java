package com.taoji666.gulimall.product.service.impl;

import com.taoji666.gulimall.product.service.CategoryBrandRelationService;
import com.taoji666.gulimall.product.dao.BrandDao;
import com.taoji666.gulimall.product.entity.BrandEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import com.taoji666.gulimall.product.service.BrandService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        //1、通过params 将前端表单的key（需要索引的关键字）传过来
        String key = (String) params.get("key");
        //new 一个 查询条件对象
        QueryWrapper<BrandEntity> queryWrapper = new QueryWrapper<>();
        //看有没有关键字传过来，有就用关键字查，将查询条件 封装进 queryWrapper。如果没有关键字传来，查询条件为空
        if(!StringUtils.isEmpty(key)){
            queryWrapper.eq("brand_id",key).or().like("name",key);
        }

        //这个IPage 会被传送到配置的分页插件对象所使用。所以一定要配置分页插件

        //2、将查询到的结果交给分页插件处理
        IPage<BrandEntity> page = this.page(
                //1、new一个查询，用key查或者key为空全查，查询条件是queryWrapper（如果有封装条件的话）
                new Query<BrandEntity>().getPage(params),
                queryWrapper

        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void updateDetail(BrandEntity brand) {
        //保证冗余字段的数据一致
        this.updateById(brand); //本来是autowired自己的Dao，然后用dao.updatedById 这里简写了，因为this里面也有这个方法
        if(!StringUtils.isEmpty(brand.getName())){//如果品牌名不为空
            /*同步更新其他关联表中的数据。
            只是冗余设计了brandId 和 brandName 两个字段，所以只要同步更新这两个就好
            updateBrand方法是自己编写的，需要自行点进去看代码
            */
            categoryBrandRelationService.updateBrand(brand.getBrandId(),brand.getName());

            //TODO 更新其他关联
        }
    }

    @Override
    public List<BrandEntity> getBrandsByIds(List<Long> brandIds) {


        return baseMapper.selectList(new QueryWrapper<BrandEntity>().in("brandId",brandIds));
    }

}