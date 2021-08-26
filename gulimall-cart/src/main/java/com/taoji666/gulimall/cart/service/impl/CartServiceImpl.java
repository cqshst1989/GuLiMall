package com.taoji666.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.taoji666.common.utils.R;
import com.taoji666.gulimall.cart.feign.ProductFeignService;
import com.taoji666.gulimall.cart.interceptor.CartInterceptor;
import com.taoji666.gulimall.cart.service.CartService;
import com.taoji666.gulimall.cart.vo.CartItemVo;
import com.taoji666.gulimall.cart.vo.CartVo;
import com.taoji666.gulimall.cart.to.UserInfoTo;
import com.taoji666.gulimall.cart.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static com.taoji666.common.constant.CartConstant.CART_PREFIX;

/**
 * @Description:
 * @author: TaoJi
 * @createTime: 2021-08-25 13:11
 **/
@Slf4j
@Service("cartService")
public class CartServiceImpl implements CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    /*
    * 添加 skuId 商品 num 件 到购物车
    * */
    @Override //传来的是商品ID ，和 购买数量
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {

        //拿到要操作的购物车信息  getCartOps()会判断好事临时购物车 还是 用户购物车。 并封装进redis的BoundHashOperations
        //<String, Object, Object> 购物车1级String  商品2级Object  商品属性3级Object
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        //判断Redis是否有该商品的信息
        String productRedisValue = (String) cartOps.get(skuId.toString());
        //如果购物车无此商品，就要添加此商品
        if (StringUtils.isEmpty(productRedisValue)) {

            //2、添加新的商品到购物车(redis)
            CartItemVo cartItemVo = new CartItemVo();
            //开启第一个异步任务  getSkuInfoFuture第一个任务的名字
            CompletableFuture<Void> getSkuInfoFuture = CompletableFuture.runAsync(() -> {
                //1、通过skuId远程查询当前要添加商品的信息（各个属性）
                R productSkuInfo = productFeignService.getInfo(skuId);
                SkuInfoVo skuInfo = productSkuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                //设置购物项的各个参数（即将sku的各个属性复制到购物项中）
                cartItemVo.setSkuId(skuInfo.getSkuId());
                cartItemVo.setTitle(skuInfo.getSkuTitle());
                cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                cartItemVo.setPrice(skuInfo.getPrice());
                cartItemVo.setCount(num);
            }, executor);

            //开启第二个异步任务  getSkuAttrValuesFuture是任务的名字，没有返回值
            CompletableFuture<Void> getSkuAttrValuesFuture = CompletableFuture.runAsync(() -> {
                //2、远程查询skuAttrValues组合信息（32G，玫瑰金）
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItemVo.setSkuAttrValues(skuSaleAttrValues);
            }, executor);

            //等待所有的异步任务全部完成
            CompletableFuture.allOf(getSkuInfoFuture, getSkuAttrValuesFuture).get();

            //存入redis之前，先将对象序列化成json
            String cartItemJson = JSON.toJSONString(cartItemVo);

            //cartOps是绑定好购物车的key的redis工具。在当前redis购物车（不管是临时 还是 用户购物车）添加商品ID，以及对应的属性
            cartOps.put(skuId.toString(), cartItemJson);

            return cartItemVo; //将商品信息返回,最终给前端页面提取
        } else { //购物车有此商品，修改数量即可
            //将JsonString 逆转成 java对象
            CartItemVo cartItemVo = JSON.parseObject(productRedisValue, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + num);//原数量 添加上新增加的数量 num 即可
            //将修改后的对象转回Json，再重新保存进redis
            String cartItemJson = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), cartItemJson);

            return cartItemVo;
        }
    }


    //获取具体购物项
    @Override
    public CartItemVo getCartItem(Long skuId) {
        //拿到redis中 要操作的购物车的key（通过redis工具操作）  （getCartOps会判断好，到底是临时购物车还是用户购物车）
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String redisValue = (String) cartOps.get(skuId.toString());

        return JSON.parseObject(redisValue, CartItemVo.class);
    }

    /**
     * 获取 临时购物车 或者 用户购物车里  所有的数据
     *
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {

        CartVo cartVo = new CartVo();

        //从同一线程中的拦截器中获取到userInfoTo（拦截器判定了用户是否已经登录）,
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();

        //1、如果用户已经登录
        if (userInfoTo.getUserId() != null) {

            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            //临时购物车的key
            String temptCartKey = CART_PREFIX + userInfoTo.getUserKey();

            //1.1、如果临时购物车的数据还未进行合并     getCartItems获取购物车的所有购物项
            List<CartItemVo> tempCartItems = getCartItems(temptCartKey); //获取临时购物车的所有购物项
            if (tempCartItems != null) {
                //临时购物车有数据需要进行合并操作
                for (CartItemVo item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());
                }
                //清除临时购物车的数据
                clearCartInfo(temptCartKey);
            }

            //1.2、获取登录后的购物车数据【包含合并过来的临时购物车的数据和登录后购物车的数据】
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cartVo.setItems(cartItems);

        //2、如果用户没登录
        } else {

            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            //获取临时购物车里面的所有购物项
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cartVo.setItems(cartItems);
        }

        return cartVo;
    }

    /**
     * 判定是 临时购物车 还是 用户购物车
     * 并将这个我们要操作的购物车 绑定好 redisKey
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        //先从线程中得到当前用户信息：拦截器中判定的当前用户 是 临时 or 登录用户
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();

        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            //如果是登录用户
            cartKey = CART_PREFIX + userInfoTo.getUserId(); //CART_PREFIX：存入redis时的前缀：gulimall:cart:
        } else { //如果是 临时用户
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }

        //redis自带方法， 绑定cartkey操作Redis
        return redisTemplate.boundHashOps(cartKey);
    }


    /**
     * 获取购物车里面的所有购物项
     *
     * @param cartKey   通过cartKey 判断时获取用户购物车的项 还是 临时购物车的所有项
     * @return
     */
    private List<CartItemVo> getCartItems(String cartKey) {
      //先从redis中取出购物车中的商品
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        List<Object> values = operations.values();

        if (values != null && values.size() > 0) {
            //将redis中的 存的商品信息，转换成 javaBean 即 CartItemVo
            List<CartItemVo> collect = values.stream().map((obj)->{
                String str = (String) obj;//我们知道 redis中 存的是 json，强转成String
                CartItemVo cartItemVo = JSON.parseObject(str,CartItemVo.class); //转成java对象
                return cartItemVo;
            }).collect(Collectors.toList());
            return collect;


            /* 可读性差的 超级简便写法
            return values.stream().map((obj) -> {
                String str = (String) obj;
                return JSON.parseObject(str, CartItemVo.class);
            }).collect(Collectors.toList());*/
        }
        return null;

    }

    //清除购物车
    @Override
    public void clearCartInfo(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {

        //查询购物车里面的商品
        CartItemVo cartItem = getCartItem(skuId);
        //修改商品状态
        cartItem.setCheck(check == 1?true:false);

        //序列化存入redis中
        String redisValue = JSON.toJSONString(cartItem);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), redisValue);

    }

    /**
     * 修改购物项数量
     *
     * @param skuId
     * @param num
     */
    @Override
    public void changeItemCount(Long skuId, Integer num) {

        //查询购物车里面的商品
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCount(num);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //序列化存入redis中
        String redisValue = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), redisValue);
    }


    /**
     * 删除购物项
     *
     * @param skuId
     */
    @Override
    public void deleteIdCartInfo(Integer skuId) {

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItemVo> getUserCartItems() {

        List<CartItemVo> cartItemVoList = new ArrayList<>();
        //获取当前用户登录的信息
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        //如果用户未登录直接返回null
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            //获取购物车项
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            //获取所有的
            List<CartItemVo> cartItems = getCartItems(cartKey);
            if (cartItems == null) {
                throw new CartExceptionHandler();
            }
            //筛选出选中的
            cartItemVoList = cartItems.stream()
                    .filter(CartItemVo::getCheck)
                    .peek(item -> {
                        //更新为最新的价格（查询数据库）
                        BigDecimal price = productFeignService.getPrice(item.getSkuId());
                        item.setPrice(price);
                    }).collect(Collectors.toList());
        }

        return cartItemVoList;
    }
}
