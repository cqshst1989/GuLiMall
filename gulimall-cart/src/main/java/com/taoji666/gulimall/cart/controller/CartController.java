package com.taoji666.gulimall.cart.controller;

import com.taoji666.gulimall.cart.service.CartService;
import com.taoji666.gulimall.cart.vo.CartItemVo;
import com.taoji666.gulimall.cart.vo.CartVo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @Description:
 * @author: TaoJi
 * @createTime: 2021-08-25 16:31
 **/
@Controller
public class CartController {

    @Resource
    private CartService cartService;

    /**
     * 获取当前用户的购物车商品项
     *
     * @return
     */
    @GetMapping(value = "/currentUserCartItems")
    @ResponseBody
    public List<CartItemVo> getCurrentCartItems() {

        List<CartItemVo> cartItemVoList = cartService.getUserCartItems();

        return cartItemVoList;
    }

    /**
     * 从首页去购物车专属页面  （相当于在京东首页点击购物车）
     *
     *
     * 京东确认临时用户的办法：
     *   给浏览器一个cookie:user-key 标识用户的身份，一个月过期
     * 如果第一次使用jd的购物车功能，都会给一个临时的用户身份，浏览器以后保存，每次访问都会带上这个cookie；
     *
     * 总结：
     * 用户登录后：服务器session有购物车信息
     * 用户没登录：按照cookie里面带来user-key来  识别购物车信息
     * 第一次登录，自动创建一个临时用户
     *
     * 考虑方案：
     *   在执行目标方法之前，判断用户的登录状态，并封装传递给controller目标请求
     *
     * @return
     */
    @GetMapping(value = "/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        //快速得到用户信息：id,user-key。toThreadLocal：同一个线程共享数据
         //UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();  //public static声明，所以这里直接可以获取到

        CartVo cartVo = cartService.getCart();
        model.addAttribute("cart", cartVo);
        return "cartList";
    }

    /**
     * 添加商品到购物车，完成功能：
     *1、添加什么商品进购物车    @param skuId
     *2、添加多少件   @param model

     */
    @GetMapping(value = "/addCartItem")
    public String addCartItem(@RequestParam("skuId") Long skuId,
                              @RequestParam("num") Integer num,
                              RedirectAttributes attributes) throws ExecutionException, InterruptedException {

        cartService.addToCart(skuId, num);

        //重定向的addToCartSuccessPage会再查一次商品信息，因此需要带上skuId
        //addAttribute会在URL后面添加请求参数，以便被其他方法的@RequestParam取出。注意区别以前重定向的addFlashAttribute

        /*
        (1)attributes.addFlashAttribute():将数据放在session中，可以在页面中取出，但是只能取一次
        (2)attributes.addAttribute():将数据放在url后面
        */

        attributes.addAttribute("skuId", skuId);

        //添加完购物车就重定向到`添加成功`页面，避免前端刷新，就重复添加购物车。（重定向，网页URL变了，刷新也是刷新重定向页面）
        return "redirect:http://cart.gulimall.com/addToCartSuccessPage.html";
    }

    /**
     * 添加商品到购物车成功
     *
     * 添加完购物车后，跳转到本成功页面，避免前端刷新页面造成购物车重复添加
     * 前端刷新，也是刷新本微服务了，本微服务就查询出数据给他返回success
     *
     */
    @GetMapping(value = "/addToCartSuccessPage.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,
                                       Model model) {
        //重定向到成功页面。再次查询购物车数据即可
        CartItemVo cartItemVo = cartService.getCartItem(skuId);
        model.addAttribute("cartItem", cartItemVo);
        return "success";
    }


    /**
     * 商品是否选中。 前端如果 选中 或者 没选中某个 商品 checked
     * 这里就需要去redis中 修改该商品对应的 属性
     *
     * 购物车页面  勾选购物项
     * @param skuId   商品Id
     * @param checked  商品是否被选中
     * @return
     */
    @GetMapping(value = "/checkItem")
    public String checkItem(@RequestParam(value = "skuId") Long skuId,
                            @RequestParam(value = "checked") Integer checked) {

        cartService.checkItem(skuId, checked);

        return "redirect:http://cart.gulimall.com/cart.html";  //重定向到购物车页面

    }


    /**
     * 改变商品数量
     *
     * @param skuId
     * @param num
     * @return
     */
    @GetMapping(value = "/countItem")
    public String countItem(@RequestParam(value = "skuId") Long skuId,
                            @RequestParam(value = "num") Integer num) {

        cartService.changeItemCount(skuId, num);

        return "redirect:http://cart.gulimall.com/cart.html";
    }


    /**
     * 删除商品信息
     *
     * @param skuId
     * @return
     */
    @GetMapping(value = "/deleteItem")
    public String deleteItem(@RequestParam("skuId") Integer skuId) {

        cartService.deleteIdCartInfo(skuId);

        return "redirect:http://cart.gulimall.com/cart.html";

    }

}
