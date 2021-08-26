package com.taoji666.gulimall.product.service.impl;

import com.taoji666.gulimall.product.dao.SkuImagesDao;
import com.taoji666.gulimall.product.entity.SkuImagesEntity;
import com.taoji666.gulimall.product.service.SkuImagesService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;


@Service("skuImagesService")
public class SkuImagesServiceImpl extends ServiceImpl<SkuImagesDao, SkuImagesEntity> implements SkuImagesService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuImagesEntity> page = this.page(
                new Query<SkuImagesEntity>().getPage(params),
                new QueryWrapper<SkuImagesEntity>()
        );

        return new PageUtils(page);
    }

    //通过skuId 查到List<SkuImagesEntity>
    @Override
    public List<SkuImagesEntity> getImagesBySkuId(Long skuId) {

        SkuImagesDao imagesDao = this.baseMapper;

        List<SkuImagesEntity> imagesEntities = imagesDao.selectList(new QueryWrapper<SkuImagesEntity>().eq("sku_id",skuId));

        return imagesEntities;
    }

}