package com.taoji666.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.taoji666.common.exception.NoStockException;
import com.taoji666.common.to.mq.OrderTo;
import com.taoji666.common.to.mq.StockDetailTo;
import com.taoji666.common.to.mq.StockLockedTo;
import com.taoji666.common.utils.R;
import com.taoji666.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.taoji666.gulimall.ware.entity.WareOrderTaskEntity;
import com.taoji666.gulimall.ware.enume.OrderStatusEnum;
import com.taoji666.gulimall.ware.enume.WareTaskStatusEnum;
import com.taoji666.gulimall.ware.feign.OrderFeignService;
import com.taoji666.gulimall.ware.feign.ProductFeignService;
import com.taoji666.gulimall.ware.dao.WareSkuDao;
import com.taoji666.gulimall.ware.entity.WareSkuEntity;
import com.taoji666.gulimall.ware.service.WareOrderTaskDetailService;
import com.taoji666.gulimall.ware.service.WareOrderTaskService;
import com.taoji666.gulimall.ware.service.WareSkuService;
import com.taoji666.gulimall.ware.vo.OrderItemVo;
import com.taoji666.gulimall.ware.vo.SkuHasStockVo;
import com.taoji666.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

@RabbitListener(queues = "stock.release.stock.queue")//监听普通队列
@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WareOrderTaskService wareOrderTaskService;

    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    private OrderFeignService orderFeignService;



    /**
     *    1、没有这个订单，必须解锁库存
     *          *          2、有这个订单，不一定解锁库存
     *          *              订单状态：已取消：解锁库存
     *          *                      已支付：不能解锁库存
     * 消息队列解锁库存
     * @param stockLockedTo
     */
    @Override
    public void unlock(StockLockedTo stockLockedTo) {
        StockDetailTo detailTo = stockLockedTo.getDetailTo();
        WareOrderTaskDetailEntity detailEntity = wareOrderTaskDetailService.getById(detailTo.getId());
        //1.如果工作单详情不为空，说明该库存锁定成功
        if (detailEntity != null) {
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(stockLockedTo.getId());
            R r = orderFeignService.infoByOrderSn(taskEntity.getOrderSn());
            if (r.getCode() == 0) {
                OrderTo order = r.getData("order", new TypeReference<OrderTo>() {
                });
                //没有这个订单||订单状态已经取消 解锁库存
                if (order == null||order.getStatus()== OrderStatusEnum.CANCLED.getCode()) {
                    //为保证幂等性，只有当工作单详情处于被锁定的情况下才进行解锁
                    if (detailEntity.getLockStatus()== WareTaskStatusEnum.Locked.getCode()){
                        unlockStock(detailTo.getSkuId(), detailTo.getSkuNum(), detailTo.getWareId(), detailEntity.getId());
                    }
                }
            }else {
                //消息拒绝以后，重新放到队列里面，让别人继续消费解锁
                throw new RuntimeException("远程调用订单服务失败");
            }
        }else {
            //无需解锁
        }
    }
    /*
    * 防止订单卡顿，导致订单状态消息一直改不了，库存消息优先到期，查订单状态新建状态，什么都不做就走了
    * 最终卡了的订单，永远无法解锁库存
    *
    * */
    @Override
    public void unlock(OrderTo orderTo) {
        //为防止重复解锁，需要重新查询工作单
        String orderSn = orderTo.getOrderSn();
        WareOrderTaskEntity taskEntity = wareOrderTaskService.getBaseMapper().selectOne((new QueryWrapper<WareOrderTaskEntity>().eq("order_sn", orderSn)));
        //查询出当前订单相关的且处于锁定状态的工作单详情
        List<WareOrderTaskDetailEntity> lockDetails = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", taskEntity.getId()).eq("lock_status", WareTaskStatusEnum.Locked.getCode()));
        for (WareOrderTaskDetailEntity lockDetail : lockDetails) {
            unlockStock(lockDetail.getSkuId(),lockDetail.getSkuNum(),lockDetail.getWareId(),lockDetail.getId());
        }
    }

    private void unlockStock(Long skuId, Integer skuNum, Long wareId, Long detailId) {
        //数据库中解锁库存数据
        baseMapper.unlockStock(skuId, skuNum, wareId);
        //更新库存工作单详情的状态
        WareOrderTaskDetailEntity detail = WareOrderTaskDetailEntity.builder()
                .id(detailId)
                .lockStatus(2).build();
        wareOrderTaskDetailService.updateById(detail);
    }







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


    /*
    * 为某个订单解锁库存
    * （1）下订单成功，订单过期没有支付被系统自动取消、被用户手动取消。都要解锁库存
    *
    * （2）下订单成功，库存锁定成功，运行过程中某些代码执行失败，导致订单回滚。
    * 之前锁定的库存就要自动解锁
    *
    * */
    @Transactional //(rollbackFor=NoStockException.class)，这个属性可以不用写，因为默认运行时异常都会回滚
    @Override
    public Boolean orderLockStock(WareSkuLockVo wareSkuLockVo) {

        /*

        *将工作单存入专门的数据表wms_ware_order_task_detail
        * 因为可能出现订单回滚后，库存锁定不回滚的情况，但订单已经回滚，得不到库存锁定信息，因此要有库存工作单
        */
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
            //找出这个商品，在哪些仓库有库存  自己写SQL语句
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
                    //返回多少行受影响，如果是0行，就说明锁定失败了  自己写SQL语句
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
                        //如果锁成功，就发送库存锁定消息至MQ的延迟队列
                        StockLockedTo lockedTo = new StockLockedTo();
                        lockedTo.setId(taskEntity.getId());
                        StockDetailTo detailTo = new StockDetailTo();
                        BeanUtils.copyProperties(detailEntity,detailTo);
                        lockedTo.setDetailTo(detailTo);

                        //将当前商品锁定了几件的工作单记录发给MQ（MyRabbitmqConfig中）     发送给stockEventExchange交换机，stock.locked键会将你送到死信队列
                        //以便如果20分钟都不付钱，MQ就可以解锁库存
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