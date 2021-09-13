package com.taoji666.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.taoji666.common.constant.CartConstant;
import com.taoji666.common.exception.NoStockException;
import com.taoji666.common.to.SkuHasStockVo;
import com.taoji666.common.to.mq.OrderTo;
import com.taoji666.common.utils.R;
import com.taoji666.common.vo.MemberResponseVo;
import com.taoji666.gulimall.order.constant.OrderConstant;
import com.taoji666.gulimall.order.dao.OrderDao;
import com.taoji666.gulimall.order.entity.OrderEntity;
import com.taoji666.gulimall.order.entity.OrderItemEntity;
import com.taoji666.gulimall.order.entity.PaymentInfoEntity;
import com.taoji666.gulimall.order.enume.OrderStatusEnum;
import com.taoji666.gulimall.order.feign.CartFeignService;
import com.taoji666.gulimall.order.feign.MemberFeignService;
import com.taoji666.gulimall.order.feign.ProductFeignService;
import com.taoji666.gulimall.order.feign.WareFeignService;
import com.taoji666.gulimall.order.interceptor.LoginInterceptor;
import com.taoji666.gulimall.order.service.OrderItemService;
import com.taoji666.gulimall.order.service.OrderService;
import com.taoji666.gulimall.order.service.PaymentInfoService;
import com.taoji666.gulimall.order.to.OrderCreateTo;
import com.taoji666.gulimall.order.to.SpuInfoTo;
import com.taoji666.gulimall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentInfoService paymentInfoService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    //给前端订单页面需要展示的OrderConfirmVo 填充属性值
    @Override
    public OrderConfirmVo confirmOrder() {
        //从拦截器的线程中ThreadLocal，获取到用户
        MemberResponseVo memberResponseVo = LoginInterceptor.loginUser.get();
        //先准备好一个 OrderConfirmVo
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        /*
        我们需要在请求上下文中找到请求头，并从中取出cookie，设置进新的feign请求，确保feign远程调用，不丢失用户登录信息
        *
        但是feign底层源码是从ThreadLocal中获取上下文，这样在多线程即 `异步` 方式中，用feign在进行远程调用，会丢失请求的上下文。（如果没用`异步`，就不会丢）
        *
        * 因此在进入`异步`方式前，即主线程中，先从ThreadLocal获取到请求上下文，然后再去每个线程 将 上下文 设置进去，就每个线程都有上下文了
        * */

        //获取到请求上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();



        CompletableFuture<Void> itemAndStockFuture = CompletableFuture.supplyAsync(() -> {
            //将请求上下文共享进当前线程
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //1. 查出购物车中所有选中购物项 设置OrderItemVo属性
            List<OrderItemVo> checkedItems = cartFeignService.getCheckedItems();
            confirmVo.setItems(checkedItems);
            return checkedItems;

            //查出购物项后，接着thenAcceptAsync 继续查 该购物项的库存。 要继续使用前面的查询结果 checkedItems，这里是items
        }, executor).thenAcceptAsync((items) -> {
            //4. 库存
            //拿到每个货物的skuId
            List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());

            //查对应的skuId是否有库存，并转换成Map
            Map<Long, Boolean> hasStockMap = wareFeignService.getSkuHasStockNR(skuIds).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
            confirmVo.setStocks(hasStockMap);//设置进confirmVo，以便前端获取是否有库存
        }, executor);

        //2. 查出所有收货地址，  本方法不依赖其他方法的结果，因此可以独立一个线程来做
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            //每一个线程，都要单独共享下上下文
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> addressByUserId = memberFeignService.getAddressByUserId(memberResponseVo.getId());
            confirmVo.setMemberAddressVos(addressByUserId);
        }, executor);

        //3. 积分  即设置integration属性
        confirmVo.setIntegration(memberResponseVo.getIntegration());

        //5. 总价自动计算（前端js算，我们就提供好数据即可）

        //6. 防重令牌
        //uuid生成的随机数里面有短横线- 替换掉
        String token = UUID.randomUUID().toString().replace("-", "");
        //将令牌存入redis，为了redis中的数据美观，都需要前缀，前缀就用常量OrderConstant.USER_ORDER_TOKEN_PREFIX+用户Id，值就是token，过期时间30分钟
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);//将防重令牌也存入confirmVo，给前端页面使用，当然是hidden，前端下次提交的时候，就可以带上这个token了
        try {
            //这里要等itemAndStockFuture 和 addressFuture 两个任务都做完后，程序再继续往下走
            CompletableFuture.allOf(itemAndStockFuture, addressFuture).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return confirmVo;
    }

    //提交订单：
    // 创建订单，验证令牌，验证价格，锁定库存后，返回订单信息
    /*@GlobalTransactional  高并发不适合用这个，这个会加很多锁，一个一个的下订单会严重影响效率。
    * 解决办法：消息队列，兔子MQ
    * */

    @Transactional//(isolation=Isolation.REPEATABLE_READ) 设置事务隔离级别
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo) {
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        //1. 验证防重令牌
        //先从拦截器里面，拿到令牌
        MemberResponseVo memberResponseVo = LoginInterceptor.loginUser.get();
        //令牌的 删除 和 对比 必须保证原子性，使用redis自带脚本来完成 对比 和 删除 功能，copy即可
        //脚本：get一个key，如果刚好等于传过来的参数ARGV，就会删除该KEY，并返回1，如果失败，则返回0
        String script= "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        //执行脚本：即原子验证令牌和删除令牌
        Long execute = redisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), submitVo.getOrderToken());
        //脚本返回0 说明验证失败
        if (execute == 0L) {
            //加入错误代码，直接返回。controller发现状态码是失败的，就会重定向到toTrade
            responseVo.setCode(1);
            return responseVo;
        //脚本验证成功，即令牌验证成功，继续往下做下单逻辑
        }else {
            //2. 创建订单、订单项。   先通过createOrderTo方法，取出To需要的memberResponseVo和submitVo中的数据
            OrderCreateTo order =createOrderTo(memberResponseVo,submitVo);

            //3. 验价  允许0.01的误差
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = submitVo.getPayPrice();
            //abs求绝对值
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //4. 保存订单
                saveOrder(order);

                //5. 锁定库存
                //重新封装后的orderItemVos 只有商品ID 和 锁定数量
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId()); //锁定的商品ID
                    orderItemVo.setCount(item.getSkuQuantity()); //锁几件商品
                    return orderItemVo;
                }).collect(Collectors.toList());
                //刚刚抽取的商品Id 和 锁定商品数量  转到专门的lockVo里面
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                lockVo.setLocks(orderItemVos);
                //锁库存只需要商品Id 和 锁定数量，因此lockVo只有这两个
                R r = wareFeignService.orderLockStock(lockVo); //远程方法用了 兔子MQ,这样即使锁定了库存，客户一直不付钱，兔子可以自行取消库存
                //5.1 锁定库存成功
                if (r.getCode()==0){
//                    int i = 10 / 0;
                    responseVo.setOrder(order.getOrder());
                    responseVo.setCode(0);

                    //发送消息到订单延迟队列，判断过期订单. 发送信息给MQ。
                    //将消息发送给order-event-exchange交换机，然后交换机沿着order.create.order路由键 送到死信队列order.delay.queue
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder());

                    //清除购物车记录
                    BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(CartConstant.CART_PREFIX + memberResponseVo.getId());
                    for (OrderItemEntity orderItem : order.getOrderItems()) {
                        ops.delete(orderItem.getSkuId().toString());
                    }
                    return responseVo;


                }else {
                    //5.1 锁定库存失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg); //一定要抛异常，抛异常才会让整个事务回滚，所有数据表都清除
                }
            //验价失败
            }else {
                responseVo.setCode(2);
                return responseVo;
            }
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));

        return order_sn;
    }

    /**
     * 关闭过期的的订单
     * @param orderEntity
     */
    @Override
    public void closeOrder(OrderEntity orderEntity) {
        //因为消息发送过来的订单已经是很久前的了，中间可能被改动，因此要查询最新的订单
        OrderEntity newOrderEntity = this.getById(orderEntity.getId());
        //如果订单还处于新创建的状态，说明超时未支付，进行关单
        if (newOrderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity updateOrder = new OrderEntity();
            updateOrder.setId(newOrderEntity.getId());
            updateOrder.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(updateOrder);

            //关单后发送消息通知其他服务进行关单相关的操作，如解锁库存
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(newOrderEntity,orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other",orderTo);
        }
    }

    @Override
    public PageUtils getMemberOrderPage(Map<String, Object> params) {
        MemberResponseVo memberResponseVo = LoginInterceptor.loginUser.get();
        QueryWrapper<OrderEntity> queryWrapper = new QueryWrapper<OrderEntity>().eq("member_id", memberResponseVo.getId()).orderByDesc("create_time");
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),queryWrapper
        );
        List<OrderEntity> entities = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItems(orderItemEntities);
            return order;
        }).collect(Collectors.toList());
        page.setRecords(entities);
        return new PageUtils(page);
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        PayVo payVo = new PayVo();
        payVo.setOut_trade_no(orderSn);
        BigDecimal payAmount = orderEntity.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(payAmount.toString());

        List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity orderItemEntity = orderItemEntities.get(0);
        payVo.setSubject(orderItemEntity.getSkuName());
        payVo.setBody(orderItemEntity.getSkuAttrsVals());
        return payVo;
    }

    @Override
    public void handlerPayResult(PayAsyncVo payAsyncVo) {
        //保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        String orderSn = payAsyncVo.getOut_trade_no();
        infoEntity.setOrderSn(orderSn);
        infoEntity.setAlipayTradeNo(payAsyncVo.getTrade_no());
        infoEntity.setSubject(payAsyncVo.getSubject());
        String trade_status = payAsyncVo.getTrade_status();
        infoEntity.setPaymentStatus(trade_status);
        infoEntity.setCreateTime(new Date());
        infoEntity.setCallbackTime(payAsyncVo.getNotify_time());
        paymentInfoService.save(infoEntity);

        //判断交易状态是否成功
        if (trade_status.equals("TRADE_SUCCESS") || trade_status.equals("TRADE_FINISHED")) {
            baseMapper.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode(), PayConstant.ALIPAY);
        }
    }

    @Transactional
    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {
        MemberResponseVo memberResponseVo = LoginInterceptor.loginUser.get();
        //1. 创建订单
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderTo.getOrderSn());
        orderEntity.setMemberId(orderTo.getMemberId());
        if (memberResponseVo!=null){
            orderEntity.setMemberUsername(memberResponseVo.getUsername());
        }
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setCreateTime(new Date());
        orderEntity.setPayAmount(orderTo.getSeckillPrice().multiply(new BigDecimal(orderTo.getNum())));
        this.save(orderEntity);
        //2. 创建订单项
        R r = productFeignService.info(orderTo.getSkuId());
        if (r.getCode() == 0) {
            SeckillSkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SeckillSkuInfoVo>() {
            });
            OrderItemEntity orderItemEntity = new OrderItemEntity();
            orderItemEntity.setOrderSn(orderTo.getOrderSn());
            orderItemEntity.setSpuId(skuInfo.getSpuId());
            orderItemEntity.setCategoryId(skuInfo.getCatalogId());
            orderItemEntity.setSkuId(skuInfo.getSkuId());
            orderItemEntity.setSkuName(skuInfo.getSkuName());
            orderItemEntity.setSkuPic(skuInfo.getSkuDefaultImg());
            orderItemEntity.setSkuPrice(skuInfo.getPrice());
            orderItemEntity.setSkuQuantity(orderTo.getNum());
            orderItemService.save(orderItemEntity);
        }
    }
    //保存订单
    private void saveOrder(OrderCreateTo orderCreateTo) {
        OrderEntity order = orderCreateTo.getOrder();
        order.setCreateTime(new Date());
        order.setModifyTime(new Date());
        this.save(order);
        orderItemService.saveBatch(orderCreateTo.getOrderItems());
    }

    private OrderCreateTo createOrderTo(MemberResponseVo memberResponseVo, OrderSubmitVo submitVo) {
        //用IdWorker生成订单号，订单号不是普通的随机数，用IdWorker生成的订单号很科学
        //特别注意：订单号长度很长，Mysql默认的char 32是不够的，要改成char 64
        String orderSn = IdWorker.getTimeId();
        //创建订单
        OrderEntity entity = buildOrder(memberResponseVo, submitVo,orderSn);
        //构建订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        //重新计算价格,用于验价
        compute(entity, orderItemEntities);
        OrderCreateTo createTo = new OrderCreateTo();
        createTo.setOrder(entity);
        createTo.setOrderItems(orderItemEntities);
        return createTo;
    }

    private void compute(OrderEntity entity, List<OrderItemEntity> orderItemEntities) {
        //总价
        BigDecimal total = BigDecimal.ZERO;
        //优惠价格
        BigDecimal promotion=new BigDecimal("0.0");
        BigDecimal integration=new BigDecimal("0.0");
        BigDecimal coupon=new BigDecimal("0.0");
        //积分
        Integer integrationTotal = 0;
        Integer growthTotal = 0;
        //遍历所有购物项的价格，然后计算每个购物项的总额，积分优惠，打折优惠。。能获得的积分，成长值
        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            total=total.add(orderItemEntity.getRealAmount());
            promotion=promotion.add(orderItemEntity.getPromotionAmount());
            integration=integration.add(orderItemEntity.getIntegrationAmount());
            coupon=coupon.add(orderItemEntity.getCouponAmount());
            integrationTotal += orderItemEntity.getGiftIntegration();
            growthTotal += orderItemEntity.getGiftGrowth(); //交易获得的成长值
        }
        //将计算出的价格，设置进数据库
        entity.setTotalAmount(total);
        entity.setPromotionAmount(promotion);
        entity.setIntegrationAmount(integration);
        entity.setCouponAmount(coupon);
        entity.setIntegration(integrationTotal);
        entity.setGrowth(growthTotal);

        //付款价格=商品价格+运费
        entity.setPayAmount(entity.getFreightAmount().add(total));

        //设置删除状态(0-未删除，1-已删除)
        entity.setDeleteStatus(0);
    }

    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> checkedItems = cartFeignService.getCheckedItems();
        List<OrderItemEntity> orderItemEntities = checkedItems.stream().map((item) -> {
            OrderItemEntity orderItemEntity = buildOrderItem(item);
            //1) 设置订单号
            orderItemEntity.setOrderSn(orderSn);
            return orderItemEntity;
        }).collect(Collectors.toList());
        return orderItemEntities;
    }

    private OrderItemEntity buildOrderItem(OrderItemVo item) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        Long skuId = item.getSkuId();
        //2) 设置sku相关属性
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(item.getTitle());
        //Spring工具类，将集合属性List<String> skuAttrValues，拆成字符串组合，以;来分割
        orderItemEntity.setSkuAttrsVals(StringUtils.collectionToDelimitedString(item.getSkuAttrValues(), ";"));
        orderItemEntity.setSkuPic(item.getImage());
        orderItemEntity.setSkuPrice(item.getPrice());
        orderItemEntity.setSkuQuantity(item.getCount());
        //3) 通过skuId查询spu相关属性并设置
        R r = productFeignService.getSpuBySkuId(skuId);
        if (r.getCode() == 0) {
            SpuInfoTo spuInfo = r.getData(new TypeReference<SpuInfoTo>() {
            });
            orderItemEntity.setSpuId(spuInfo.getId());
            orderItemEntity.setSpuName(spuInfo.getSpuName());
            orderItemEntity.setSpuBrand(spuInfo.getBrandName());
            orderItemEntity.setCategoryId(spuInfo.getCatalogId());
        }
        //4) 商品的优惠信息(不做)

        //5) 商品的积分成长，为价格x数量
        orderItemEntity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());
        orderItemEntity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());

        //6) 订单项订单价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

        //7) 实际价格
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity()));
        BigDecimal realPrice = origin.subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(realPrice);

        return orderItemEntity;
    }

    private OrderEntity buildOrder(MemberResponseVo memberResponseVo, OrderSubmitVo submitVo, String orderSn) {

        OrderEntity orderEntity =new OrderEntity();

        orderEntity.setOrderSn(orderSn);

        //2) 设置用户信息
        orderEntity.setMemberId(memberResponseVo.getId());
        orderEntity.setMemberUsername(memberResponseVo.getUsername());

        //3) 获取邮费和收件人信息并设置

        //远程查到收件人地址 和 运费
        FareVo fareVo = wareFeignService.getFare(submitVo.getAddrId());
        BigDecimal fare = fareVo.getFare(); //获取运费
        orderEntity.setFreightAmount(fare); //设置运费
        MemberAddressVo address = fareVo.getAddress();
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());

        //4) 设置订单相关的状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setConfirmStatus(0);
        orderEntity.setAutoConfirmDay(7);

        return orderEntity;
    }

}