package com.taoji666.gulimall.product.service.impl;

import com.taoji666.gulimall.product.dao.SkuInfoDao;
import com.taoji666.gulimall.product.entity.SkuImagesEntity;
import com.taoji666.gulimall.product.entity.SkuInfoEntity;
import com.taoji666.gulimall.product.entity.SpuInfoDescEntity;
import com.taoji666.gulimall.product.service.*;
import com.taoji666.gulimall.product.vo.SkuItemSaleAttrVo;
import com.taoji666.gulimall.product.vo.SkuItemVo;
import com.taoji666.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import org.springframework.util.StringUtils;

import javax.annotation.Resource;


/**
 * @author Taoji
 */
@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Resource
    private SkuImagesService skuImagesService;

    @Resource
    private SpuInfoDescService spuInfoDescService;

    @Resource
    private AttrGroupService attrGroupService;

    @Resource
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    ThreadPoolExecutor executor;




    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }


    /**
     * 21 SKU 检索
     * 前端请求 GET /product/skuinfo/list
     * {
     * page: 1,//当前页码
     * limit: 10,//每页记录数
     * sidx: 'id',//排序字段
     * order: 'asc/desc',//排序方式
     * key: '华为',//检索关键字
     * catelogId: 0,  检索条件
     * brandId: 0,   检索条件
     * min: 0,   检索条件
     * max: 0   检索条件
     * }  前端传来的数据有分页相关要求
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        //先写查询条件
        QueryWrapper<SkuInfoEntity> queryWrapper = new QueryWrapper<>();
        /**
         * 如果检索条件都给了，就按照检索条件检索
         * key:  检索条件
         * catelogId: 0
         * brandId: 0
         * min: 0
         * max: 0
         */
        String key = (String) params.get("key");
        //key 不为空的时候，才按照key的条件来检索
        if(!StringUtils.isEmpty(key)){
            //and的特性，已将讲过了，全文检索即可
            queryWrapper.and((wrapper)->{
               wrapper.eq("sku_id",key).or().like("sku_name",key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        //String类的equalsIgnoreCase(catelogId)  ——将字符串与指定的对象比较，不考虑大小写。
        //这里将0 与 catelogId 进行比较  即 传空值 和 传 0  进来都不行
        if(!StringUtils.isEmpty(catelogId)&&!"0".equalsIgnoreCase(catelogId)){

            queryWrapper.eq("catalog_id",catelogId);
        }

        String brandId = (String) params.get("brandId");
        //这里也不允许传0进来
        if(!StringUtils.isEmpty(brandId)&&!"0".equalsIgnoreCase(catelogId)){
            queryWrapper.eq("brand_id",brandId);
        }

        String min = (String) params.get("min");
        if(!StringUtils.isEmpty(min)){
            queryWrapper.ge("price",min);
        }

        String max = (String) params.get("max");

        if(!StringUtils.isEmpty(max)  ){
            //max如果为0 就是查找 0~0 注定没查询结果。
            try{
                BigDecimal bigDecimal = new BigDecimal(max);
                //compareTo 为返回1 0 -1 ,==1说明比0大，就执行下面的
                if(bigDecimal.compareTo(new BigDecimal("0"))==1){
                    //le 的意思是<= 即 price <= max
                    queryWrapper.le("price",max);
                }
            }catch (Exception e){

            }

        }


        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId){  //查询条件是 通过spuid来查
        List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return list;
    }

    /*
    * 展示商品详情页
    * 通过异步方式提高效率
    * 思路：
    * 1、2 完全没关系，可以同时进行
    * 3、4、5需要依赖1的结果
    * 首先需要写配置类，配置好线程池，config中完成
    * */
    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();


        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            //1、sku基本信息的获取  查询数据表pms_sku_info
            SkuInfoEntity info = this.getById(skuId);
            skuItemVo.setInfo(info);
            return info; //获取到基本信息后，将结果info  return给 3、4、5用
        }, executor);//将所有任务提交给注入的线程池executor执行

        //infoFuture.thenAcceptAsync 第一步完成后then，用第一步的结果Accept，接下来以异步的方式继续做事Async
        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> { //res就是上一步return的info
            //3、then，第三步，获取spu的销售属性组合   sku_ids 组合了不同列的所有颜色，并且去重
            /*
            ** attr_id  attr_name  attr_value  sku_ids
             * 9        颜色        白色          12,13,14
             * 9        颜色        紫色          24,25,26
             * 9        颜色        红色          21,22,23
             * 12       版本        128G         9,12,15,18,21,24
             * 12       版本        256G         10,13,16,19,22,25
            * */
            List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, executor);//完成这个任务后，不在这个任务后面继续then，继续then和串行化就没有区别了，4可以和3同时做的。

        //这一步在线程池里面找个空线程和3同时做，因此继续用infoFuture.thenAcceptAsync
        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            //4、获取spu的介绍    pms_spu_info_desc
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(spuInfoDescEntity);
        }, executor);

        //同理获取规格信息，也可以和3、4同时做，继续用infoFuture.thenAcceptAsync
        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            //5、获取spu的规格参数信息
            List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, executor);


        // Long spuId = info.getSpuId();
        // Long catalogId = info.getCatalogId();

        //2、sku的图片信息     查询数据表pms_sku_images
        //自行编写一个通过skuId找到图片的方法getImagesBySkuId(skuId)。自己写方法是因为是by别人的id，不是by自己的id
        //该任务可以和1同时开始做，因此单独开一个多线程任务，由于没返回结果，可直接用runAsync
        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> imagesEntities = skuImagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(imagesEntities);
        }, executor); //也得放在自己的线程池进行运行

       /* CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            //3、远程调用查询当前sku是否参与秒杀优惠活动
            R skuSeckilInfo = seckillFeignService.getSkuSeckilInfo(skuId);
            if (skuSeckilInfo.getCode() == 0) {
                //查询成功
                SeckillSkuVo seckilInfoData = skuSeckilInfo.getData("data", new TypeReference<SeckillSkuVo>() {
                });
                skuItemVo.setSeckillSkuVo(seckilInfoData);

                if (seckilInfoData != null) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime > seckilInfoData.getEndTime()) {
                        skuItemVo.setSeckillSkuVo(null);
                    }
                }
            }
        }, executor);*/

        //等到所有任务都完成 不用等infoFuture，因为3、4、5是基于1infofuture的结果继续做，3、4、5都完成了，1肯定完成了
        // CompletableFuture.allOf(saleAttrFuture, descFuture, baseAttrFuture, imageFuture, seckillFuture).get();
        CompletableFuture.allOf(saleAttrFuture, descFuture, baseAttrFuture, imageFuture).get();

        //阻塞等待所有任务完成，再返回结果
        return skuItemVo;
    }

}