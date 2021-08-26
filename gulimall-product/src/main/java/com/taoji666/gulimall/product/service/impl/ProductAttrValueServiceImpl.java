package com.taoji666.gulimall.product.service.impl;

import com.taoji666.gulimall.product.dao.ProductAttrValueDao;
import com.taoji666.gulimall.product.entity.ProductAttrValueEntity;
import com.taoji666.gulimall.product.service.ProductAttrValueService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import org.springframework.transaction.annotation.Transactional;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveProductAttr(List<ProductAttrValueEntity> collect) {
        this.saveBatch(collect);
    }

    /*GET  /product/attr/info/{attrId}
   商品系统19 获取spu规格    （用于修改spu的时候回显）
   要求返回的 刚好就是实体类本身，不需要vo
   由于只显示某个商品的spu，所以，也不会需要分页
*/
    @Override
    public List<ProductAttrValueEntity> baseAttrlistforspu(Long spuId) {
        List<ProductAttrValueEntity> entities = this.baseMapper.selectList(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        return entities;
    }


    /* POST  /product/attr/update/{spuId}
           商品系统 23、修改商品规格
           请求参数：  （要修改的地方）
           [{
       "attrId": 7,
       "attrName": "入网型号",
       "attrValue": "LIO-AL00",
       "quickShow": 1
    }, {
       "attrId": 14,
       "attrName": "机身材质工艺",
       "attrValue": "玻璃",
       "quickShow": 0
    }, {
       "attrId": 16,
       "attrName": "CPU型号",
       "attrValue": "HUAWEI Kirin 980",
       "quickShow": 1
    }]
    由于修改非常麻烦，最好的办法是，直接删除原来的数据，然后插入新的数据
        */
    @Transactional
    @Override
    public void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities) {
        //1、删除这个spuId之前对应的所有属性
        this.baseMapper.delete(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id",spuId));

        //让新插入的ID 和 原来删除的ID一致。其他不变，就用前端传来的那些参数
        List<ProductAttrValueEntity> collect = entities.stream().map(item -> {
            item.setSpuId(spuId);
            return item;
        }).collect(Collectors.toList());
        this.saveBatch(collect);
    }

}