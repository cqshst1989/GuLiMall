package com.taoji666.gulimall.product.service.impl;

import com.taoji666.gulimall.product.dao.SkuSaleAttrValueDao;
import com.taoji666.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.taoji666.gulimall.product.service.SkuSaleAttrValueService;
import com.taoji666.gulimall.product.vo.SkuItemSaleAttrVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }


    /*
    组装销售属性
     * attr_id  attr_name  attr_value
     * 9        颜色        白色，紫色，红色，绿色，黄色，黑色
     * 12       版本        128G，64G，256G
     * */
    @Override
    public List<SkuItemSaleAttrVo> getSaleAttrBySpuId(Long spuId) {
        SkuSaleAttrValueDao baseMapper = this.getBaseMapper();

        //给vo赋值，涉及多个数据表的属性，只能自己写SQL语句来查
        //联合查询pms_sku_info info 和 pms_sku_sale_attr_value 表
        return baseMapper.getSaleAttrBySpuId(spuId);
    }

    @Override
    public List<String> getSkuSaleAttrValuesAsStringList(Long skuId) {
        SkuSaleAttrValueDao baseMapper = this.baseMapper;
        return baseMapper.getSkuSaleAttrValuesAsStringList(skuId);
    }

}