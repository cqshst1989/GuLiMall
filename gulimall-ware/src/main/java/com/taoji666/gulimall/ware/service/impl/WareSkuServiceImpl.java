package com.taoji666.gulimall.ware.service.impl;

import com.taoji666.common.exception.NoStockException;
import com.taoji666.common.utils.R;
import com.taoji666.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.taoji666.gulimall.ware.entity.WareOrderTaskEntity;
import com.taoji666.gulimall.ware.feign.ProductFeignService;
import com.taoji666.gulimall.ware.dao.WareSkuDao;
import com.taoji666.gulimall.ware.entity.WareSkuEntity;
import com.taoji666.gulimall.ware.service.WareSkuService;
import com.taoji666.gulimall.ware.vo.OrderItemVo;
import com.taoji666.gulimall.ware.vo.SkuHasStockVo;
import com.taoji666.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import org.springframework.transaction.annotation.Transactional;
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

    //为某个订单锁库存：该订单的所有商品都要锁，要嘛都锁定成功，要嘛都不锁（事务），所有商品都锁定成功后，更改锁定状态

    @Transactional //(rollbackFor=NoStockException.class)，这个属性可以不用写，因为默认运行时异常都会回滚
    @Override
    public Boolean orderLockStock(WareSkuLockVo wareSkuLockVo) {

        //因为可能出现订单回滚后，库存锁定不回滚的情况，但订单已经回滚，得不到库存锁定信息，因此要有库存工作单
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(wareSkuLockVo.getOrderSn());
        taskEntity.setCreateTime(new Date());
        wareOrderTaskService.save(taskEntity);

        //找到每个商品 分别在 哪些仓库 有库存
        List<OrderItemVo> itemVos = wareSkuLockVo.getLocks();
        List<SkuLockVo> lockVos = itemVos.stream().map((item) -> {
            //SkuLockVo：商品id，商品数量，仓库id
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setNum(item.getCount());
            //找出这个商品，在哪些仓库有库存
            List<Long> wareIds = baseMapper.listWareIdsHasStock(item.getSkuId(), item.getCount());
            skuLockVo.setWareIds(wareIds);//保存好有库存的仓库
            return skuLockVo;
        }).collect(Collectors.toList());

        //遍历有库存的仓库，锁定里面的货
        for (SkuLockVo lockVo : lockVos) {
            boolean lock = true;
            Long skuId = lockVo.getSkuId();
            List<Long> wareIds = lockVo.getWareIds();
            //如果任何仓库都没有库存，就抛异常
            if (wareIds == null || wareIds.size() == 0) {
                throw new NoStockException(skuId);//哪个商品skuId没有库存了
            //如果仓库里面有库存，就锁定库存。即锁定字段+
            }else {
                for (Long wareId : wareIds) {
                    //lockWareSku 锁定哪个商品，锁定几件商品，是哪个仓库的
                    //返回多少行受影响，如果是0行，就说明锁定失败了
                    Long count=baseMapper.lockWareSku(skuId, lockVo.getNum(), wareId);
                    if (count==0){
                        lock=false;
                    }else {
                        //锁定成功，保存工作单详情
                        WareOrderTaskDetailEntity detailEntity = WareOrderTaskDetailEntity.builder()
                                .skuId(skuId)
                                .skuName("")
                                .skuNum(lockVo.getNum())
                                .taskId(taskEntity.getId())
                                .wareId(wareId)
                                .lockStatus(1).build();
                        wareOrderTaskDetailService.save(detailEntity);
                        //发送库存锁定消息至延迟队列
                        StockLockedTo lockedTo = new StockLockedTo();
                        lockedTo.setId(taskEntity.getId());
                        StockDetailTo detailTo = new StockDetailTo();
                        BeanUtils.copyProperties(detailEntity,detailTo);
                        lockedTo.setDetailTo(detailTo);
                        rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked",lockedTo);

                        lock = true;
                        break;
                    }
                }
            }
            if (!lock) throw new NoStockException(skuId);
        }
        return true;
    }

    @Data
    class SkuLockVo{
        private Long skuId; //商品ID
        private Integer num;  //商品数量
        private List<Long> wareIds; //商品所在的仓库ID（商品可能同时在多个仓库）
    }


}