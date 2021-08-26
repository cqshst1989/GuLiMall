package com.taoji666.gulimall.ware.service.impl;

import com.taoji666.common.utils.R;
import com.taoji666.gulimall.ware.feign.ProductFeignService;
import com.taoji666.gulimall.ware.dao.WareSkuDao;
import com.taoji666.gulimall.ware.entity.WareSkuEntity;
import com.taoji666.gulimall.ware.service.WareSkuService;
import com.taoji666.gulimall.ware.vo.SkuHasStockVo;
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

import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;


    /**
     *文档库存系统02、查询商品库存
     * 前端请求参数GET  /ware/waresku/list
     * {
     *    page: 1,//当前页码
     *    limit: 10,//每页记录数
     *    sidx: 'id',//排序字段
     *    order: 'asc/desc',//排序方式
     *    wareId: 123,//仓库id
     *    skuId: 123//商品id
     * }
     */

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        /**
         * 两个查询条件 ，并且关系
         * skuId: 1 and wareId: 2
         */
        //构造查询条件
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)){
            queryWrapper.eq("sku_id",skuId);
        }

        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)){
            queryWrapper.eq("ware_id",wareId);
        }


        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }
    //需要自己编写sql语句
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1、判断如果还没有这个库存记录      就是新增操作
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if(entities == null || entities.size() == 0){
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            //TODO 远程查询sku的名字，如果失败，整个事务无需回滚
            //1、自己catch异常
            //TODO 还可以用什么办法让异常出现以后不回滚？高级
            try {
                R info = productFeignService.info(skuId);
                Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");

                if(info.getCode() == 0){
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            }catch (Exception e){

            }


            wareSkuDao.insert(skuEntity);
        }else{
            wareSkuDao.addStock(skuId,wareId,skuNum);
        }

    }
    //库存可能在多个仓库中  而且还要减去锁定的库存，才是可用库存
    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds){



         List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
             SkuHasStockVo vo = new SkuHasStockVo();

             //查询当前sku的总库存量（各个仓库库存-已锁定库存）
             //SQL语句会用到函数 SELECT SUM(stock-stock_locked) FROM `wms_ware_sku` WHERE sku_id =  #{skuId}
             Long count = baseMapper.getSkuStock(skuId); //需要使用包装类Long，包装类才能接收null数据（mysql中该字段可能是null)

             vo.setSkuId(skuId);
             vo.setHasStock(count == null?false:count > 0);//如果count为null会产生空指针异常，因此一旦为null，需要转为false，表示没有库存，否则返回count的值
             return vo;
        }).collect(Collectors.toList());

         return collect;
    }


}