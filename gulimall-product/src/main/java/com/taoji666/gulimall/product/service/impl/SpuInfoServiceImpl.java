package com.taoji666.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.taoji666.common.constant.ProductConstant;
import com.taoji666.common.to.SkuHasStockVo;
import com.taoji666.common.to.SkuReductionTo;
import com.taoji666.common.to.SpuBoundTo;
import com.taoji666.common.to.es.SkuEsModel;
import com.taoji666.common.utils.R;
import com.taoji666.gulimall.product.feign.CouponFeignService;
import com.taoji666.gulimall.product.dao.SpuInfoDao;
import com.taoji666.gulimall.product.entity.*;
import com.taoji666.gulimall.product.feign.SearchFeignService;
import com.taoji666.gulimall.product.feign.WareFeignService;
import com.taoji666.gulimall.product.service.*;
import com.taoji666.gulimall.product.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {


    //默认修饰符是default，可不写，default修饰符下，同一个包中，都可以直接用

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService imagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService attrValueService;

    @Autowired
    SkuInfoService skuInfoService;
    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * //TODO 高级部分完善
     * @param vo
     * 对应的是商品系统19新增商品 /product/spuinfo/save
     * 将前端发布的商品涉及的各个数据  保存进各个数据库中  的数据表里
     *
     */
    /*凡是开启事务的地方，做断点调试需要特别注意：
    * 事务在提交之前，数据表是不会更新的。因此想真的调试代码，就必须暂时更改事务的隔离级别，Mysql默认隔离级别是不可重复读
    * 我们去mysql数据库中将隔离级别改成最低级，允许脏读（没提交的事务也可以读）就可以调试啦
    * set session transaction isolation level Read uncommited;
    * */
    @Transactional //需要操作非常多的表，而且数据表中都互相有关系，因此要开启事务
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {

        //1、保存spu基本信息 数据表pms_spu_info
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,infoEntity);//spring黑科技 直接复制同名属性.非同名属性直接丢弃
        infoEntity.setCreateTime(new Date());// 创建日期字段
        infoEntity.setUpdateTime(new Date());// 更新日期字段
        //自己编写的方法，该方法通过mybatis方法插入进数据表实体
        //直接对数据库动手的操作都给mapper去做
        this.saveBaseSpuInfo(infoEntity);

        //2、保存Spu的描述图片 数据表pms_spu_info_desc。从文档中，发现传来的是图片的url地址
        List<String> decript = vo.getDecript(); //从vo中取出前端传来的decript
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        //由于spu基本信息表里面的id 就是对应 该图片表的 id，因此取出id
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",",decript));//一个商品可以有多张图片。用“，”分割图片的地址
        spuInfoDescService.saveSpuInfoDesc(descEntity);//自己编写方法向数据库插入该数据



        //3、保存spu的图片集 数据表pms_spu_images
        List<String> images = vo.getImages();
        //infoEntity.getId() 表示你要保存哪个商品spuid 的图片
        imagesService.saveImages(infoEntity.getId(),images);


        //4、保存spu的规格参数; 数据表pms_product_attr_value
        //前端传来了一个baseattrs，封装了attrid，values，showDesc（1快速展示，0否），他们只是该数据表的一部分。因此用一个baseAttrsVo来先接收数据
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        //将baseAttrs 中的部分数据先存到实体中去
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setAttrId(attr.getAttrId());
            AttrEntity id = attrService.getById(attr.getAttrId());//id前端没直接传过来，需要用别人的service查一下
            valueEntity.setAttrName(id.getAttrName());
            valueEntity.setAttrValue(attr.getAttrValues());
            valueEntity.setQuickShow(attr.getShowDesc());
            valueEntity.setSpuId(infoEntity.getId());

            return valueEntity;
        }).collect(Collectors.toList());
        attrValueService.saveProductAttr(collect); //编写一个保存方法，在保存方法中用mybatis绝招


        //5、保存spu的积分信息；数据表gulimall_sms->sms_spu_bounds
        //这里处理的前端数据是 "buyBounds": 500,"growBounds": 6000
        //这里要其他数据库的数据表sms_spu_bounds了，调用其他服务的接口写在专门的feign目录下
        Bounds bounds = vo.getBounds();//新建一个vo：Bounds 来接收前端传来的两个数据

        //微服务之间传输 使用的 实体对象 TO，由于两个微服务同时会使用这个对象，因此TO一般放在公共类common中
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds,spuBoundTo);//将页面传来的数据buybounds 和 growbounds 传给TO
        spuBoundTo.setSpuId(infoEntity.getId());//TO单独需要的数据，自行设置好
        R r = couponFeignService.saveSpuBounds(spuBoundTo); //远程操作sms_spu_bounds数据表保存好数据。远程调用的是其他微服务的controller
        if(r.getCode() != 0){
            log.error("远程保存spu积分信息失败");
        }


        //6、保存当前spu对应的所有sku信息；前端传来的如下： ps可级联的又专门写了单独的vo
      /* "attr"（级联  attrId attrName attrValue）        pms_sku_sale_attr_value数据表
        "skuName": "Apple XR 黑色 6GB",     sku_info数据表
        "price": "1999",                    sku_info数据表
        "skuTitle": "Apple XR 黑色 6GB",      sku_info数据表
        "skuSubtitle": "Apple XR 黑色 6GB",   sku_info数据表
        "images": （级联 imgUrl 和 defaultImg）  pms_sku_images数据表
         "descar": ["黑色", "6GB"],
        "fullCount": 5,     sms_sku_ladder数据表
        "discount": 0.98,    sms_sku_ladder数据表
        "countStatus": 1,
        "fullPrice": 1000,   sms_sku_full_reduction 数据表
        "reducePrice": 10,   sms_sku_full_reduction 数据表
        "priceStatus": 0,
        "memberPrice": （级联）  sms_member_price数据表
       */

        List<Skus> skus = vo.getSkus(); //获取完整sku



        // 6.1）向pms_sku_info数据表保存信息
        //这一步是为 设置默认图片做铺垫，找到默认图片defaultImg
        if(skus!=null && skus.size()>0){
            skus.forEach(item->{
                String defaultImg = "";
                for (Images image : item.getImages()) {
                    if(image.getDefaultImg() == 1){ //这个defaultImg在图片数据表skuImage里设置
                        defaultImg = image.getImgUrl();
                    }
                }
                //    private String skuName;
                //    private BigDecimal price;
                //    private String skuTitle;
                //    private String skuSubtitle;
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity(); //准备更新skuInfoEntity
                BeanUtils.copyProperties(item,skuInfoEntity); //以上备注的四个属性和skuInfoEntity数据表完全对应，直接复制

                //skuInfoEntity的其他属性，就只能自己设置了。设置的值，找相关数据表SpuInfoEntity
                skuInfoEntity.setBrandId(infoEntity.getBrandId()); //infoEntity就是SpuInfoEntity的对象
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                //以上，所有pms_sku_info 表信息已经设置完，放进对应的skuInfoEntity实体中

                skuInfoService.saveSkuInfo(skuInfoEntity); //更新pms_sku_info 数据表


                //6.2）、更新sku的图片信息；即更新pms_sku_image数据表
                Long skuId = skuInfoEntity.getSkuId();//经过6.1 skuInfoEntity中已经有skuId了，取出来赋值给图片表

                //该skuId 会对应很多张图片，每行的skuId是一个定值，但是其他的 img_url，img_sort，就要变了
                //item是每一个sku完整版。这里只要图片表相关的数据
                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(img -> {
                    //从前端获取的sku中的 图片 相关数据，保存在pms_sku_images数据表
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity->{ //前端传来的商品，有的并没有图片，没有图片的，就不用保存进图片数据库
                    //返回true就是需要，false就是剔除
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());

                skuImagesService.saveBatch(imagesEntities);
                //TODO 没有图片路径的无需保存


                //6.3）、sku的销售属性信息：pms_sku_sale_attr_value数据表

                List<Attr> attr = item.getAttr(); //Attr又是一个vo

                //保存在SkuSaleAttrValueEntity这个实体对应的数据表
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, attrValueEntity);//先直接拷贝前端数据
                    attrValueEntity.setSkuId(skuId);//通过pms_sku_info数据表的skuId设置该字段

                    return attrValueEntity;
                }).collect(Collectors.toList());



                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                // 6.4）、sku的优惠、满减等信息；gulimall_sms->sms_sku_ladder\sms_sku_full_reduction\sms_member_price 三张表的字段
                //先写个to，把前端传来的这些相关数据通通收集完，只有skuId没有，需要单独设置
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item,skuReductionTo);
                skuReductionTo.setSkuId(skuId);//通过pms_sku_info数据表的skuId设置字段

                //开始远程调用
                //有满减价格才有意义，要不就不用加入数据库。 类的比较，要用专门的比较函数compareTo
                if(skuReductionTo.getFullCount() >0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1){
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if(r1.getCode() != 0){
                        log.error("远程保存sku优惠信息失败");
                    }
                }



            });
        }






    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity infoEntity) {
        this.baseMapper.insert(infoEntity);
    }

     /* 18、spu检索
     前端请求参数： GET   /product/spuinfo/list
     *{
     *    page: 1,//当前页码
     *    limit: 10,//每页记录数
     *    sidx: 'id',//排序字段
     *    order: 'asc/desc',//排序方式
     *    key: '华为',//检索关键字   检索条件
     *    catelogId: 6,//三级分类id
     *    brandId: 1,//品牌id    检索条件
     *    status: 0,//商品状态   检索条件
     * }
     */
    //查询，一般都是返回分页的到前端
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {



        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            //如果key不为空（即key是检索条件之一）
            //and方法，相当于 sql语句：status = 1 and(id=key or spu_name like key)
            /*假如不用and方法的sql语句：status=1 and id=1 or spu_name like xxx
            这样or后面的spu_name成立,前面的判断就失效 */
            wrapper.and((w)->{ //就检索 id 或者 名字 里面  有没有key
                w.eq("id",key).or().like("spu_name",key);
            });
        }
        String status = (String) params.get("status");
        if(!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status",status);
        }

        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId)&&!"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id",brandId);
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId)&&!"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id",catelogId);
        }

        /**
         * status: 2
         * key:
         * brandId: 9
         * catelogId: 225
         */

        IPage<SpuInfoEntity> page = this.page(
                //getPage方法，要取params中前端想要的分页要求 page，limit，order，sidx等
                //前端对分页没得要求（没传分页参数过来），自然就会用配置文件配置的分页要求
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }
    /*商品上架，就是给新创建的SkuEsModel赋值，最后放进ES中
    先检索（从对应数据库中查），后设置原则原则（给SkuEsModel赋值）。

     */
    @Override
    public void up(Long spuId) {
        // 1.1、检索：查出当前spuId对应的所有sku信息,品牌的名字，通过skuInfoService
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);

        //双冒号写法，Lambda，匿名对象是SkuInfoEntity的对象，的getSkuId 方法获取的返回值，再collect
        //1.2 检索：从返回的List<SkuInfoEntity> skus 中 取出 SkuId集合
        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        // 1.3、检索：通过形参spuId查出当前sku的所有可以被用来`检索`的规格属性  pms_product_attr_value 数据表中
        List<ProductAttrValueEntity> baseAttrs = attrValueService.baseAttrlistforspu(spuId);
        //查出这个sku的有多少属性 比如入网型号，上市年份,找到attrIds
        List<Long> attrIds = baseAttrs.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        //装逼双冒号写法，Lambda匿名   ProductAttrValue就是attr的类嘛
        //List<Long> attrIds = baseAttrs.stream().map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());

        //通过attrids，找到检索属性.只有15、16
        List<Long> searchAttrIds = attrService.selectSearchAttrs(attrIds);

        //将2个检索属性转换为Set集合 这样方便排除重复的
        Set<Long> idSet = new HashSet<>(searchAttrIds);

        //2.1、赋值： 将查到的attrs赋值给es对象 SkuEsModel内部类Attrs
        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream()
                .filter(item -> idSet.contains(item.getAttrId())) //包含了attrid才行
                .map(item -> {
                    SkuEsModel.Attrs attrs = new SkuEsModel.Attrs(); //SkuEsModel中的内部类
                    BeanUtils.copyProperties(item, attrs);
                    return attrs;
                }).collect(Collectors.toList());

        // 1.4 检索：发送远程调用，库存系统查询是否有库存
        //由于stockMap在try里面，因此外面必须有声明
        Map<Long,Boolean> stockMap = null;
        try {
            //远程使用库存系统的getSkuHasStock方法看是否有库存,查到库存后先赋值给SkuHasStockVo
            //查询到结果后先暂时放进R
            //返回的是 返回R的Map 属性是data，值本应是List<SkuHasStockVo> vos。可是目前vos的类型还不是List<SkuHasStockVo>，是Object，因此需要转换
            //也是通病，进入R封装的通通都是Object，每个都需要还原成原对象。主要这个是数组，要不直接强转
            R r = wareFeignService.getSkuHasStock(skuIdList); //TypeReference铺垫:这个方法会给R，setData,即把数据先放入R对象r

            //封装成map，这样用一个key，就可以读取list，更方便  形参是long 是 sku的id ， Boolean 是 这个id到底有没有东西

            //铺垫1：Typereference<转换后的对象（集合）>  getdata会获取put进R的"data" 和 Object，需要转换成List<SkuHasStockVo>
            //现将获取到的Object转换成List<SkuHasStockVo>
            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>(){};

            stockMap = r.getData(typeReference).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId,item->item.getHasStock()));
        }catch (Exception e){
            log.error("库存服务查询异常，原因{}",e);
        }

        //2.2、赋值，继续给SkuEsModel  封装每个sku的信息
       //stockMap使用过Lambda的stream表达式，因此不允许多次赋值，显然后面还在赋值，因此只能把stockMap给另一个变量finalStockMap
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            //将需要的数据封装进SkuEsModel
            SkuEsModel esModel = new SkuEsModel();

            /*先将第一步查到的SkuInfoEntity中的对应数据传入SkuEsModel
            处理原则，同属性名的直接用BeanUtils.copyProperties
            不同属性名的，一个一个的设置

            同一张数据表中的不同名属性，设置最方便，比如skuPrice和 skuImg
            同一微服务下不同数据表，也还好，直接注入他的service来查就好了
            不同微服务的数据查询最麻烦，就需要用feign了
             */
            //skuPrice不同属性名，需要单独设置
            esModel.setSkuPrice(sku.getPrice());
            //图片属性名不一样，需要单独设置  对应关系是SkuImg -- SkuDefaultImg
            esModel.setSkuImg(sku.getSkuDefaultImg());


            //  2.3、设置库存信息  先检索，后设置。
            if (finalStockMap == null){
                esModel.setHasStock(true);
            }else {
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }


            // 2.4、设置热度评分。0
            esModel.setHotScore(0L);

            // 2.5 查询品牌和分类的名字信息并设置 分别通过brandService 和  categoryService
            BrandEntity brandEntity = brandService.getById(sku.getBrandId());
            esModel.setBrandName(brandEntity.getName());
            esModel.setBrandId(brandEntity.getBrandId());
            esModel.setBrandImg(brandEntity.getLogo());

            CategoryEntity categoryEntity = categoryService.getById(sku.getCatalogId());
            esModel.setCatalogId(categoryEntity.getCatId());
            esModel.setCatalogName(categoryEntity.getName());

            // 2.6、设置检索属性
            esModel.setAttrs(attrsList);

            BeanUtils.copyProperties(sku, esModel);

            return esModel;
        }).collect(Collectors.toList());

        // 3、将数据发给es进行保存：gulimall-search （es的真正重点）
        R r = searchFeignService.productStatusUp(upProducts);

        if (r.getCode() == 0) {
            // 远程调用成功
            // 发布成功以后，需要修改数据库，修改当前spu的状态,pms_spu_info数据表的publish_status改成1 已发布
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode()); //需要自己写sql语句
        } else {
            // 远程调用失败
            // TODO 7、重复调用？接口幂等性:重试机制
            /**
             * Feign会自行搞定，Feign源码级流程如下
             * 1、构造请求数据，将对象转换为json
             * 2、发送请求执行（执行成功会解码响应数据）
             * 3、执行请求会有重试机制（可自行设置最大重试次数）
             *
             */
        }

    }


}