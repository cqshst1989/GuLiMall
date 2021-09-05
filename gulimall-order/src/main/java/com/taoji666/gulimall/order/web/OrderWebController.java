package com.taoji666.gulimall.order.web;

import com.taoji666.common.exception.NoStockException;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.order.service.OrderService;
import com.taoji666.gulimall.order.vo.OrderConfirmVo;
import com.taoji666.gulimall.order.vo.OrderSubmitVo;
import com.taoji666.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;


@Controller
public class OrderWebController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/{page}/order.html")
    public String toPage(@PathVariable("page") String page) {
        return page;
    }


    //购物车页面点击`结算`，就来到这里，将前端订单确认页要用的数据OrderConfirmVo 封装好  并返回
    @RequestMapping("/toTrade")
    public String toTrade(Model model) {
        //写一个方法confirmOrder，来获取OrderConfirmVo，用于前端展示数据
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("confirmOrder", confirmVo);
        return "confirm";
    }

    //确认好订单中的商品，以及价格后，点击`提交`订单，就来到这个请求
    @RequestMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo submitVo, Model model, RedirectAttributes attributes) {
        try{
            //SubmitOrderResponseVo用于获取到订单信息，或者错误信息
            SubmitOrderResponseVo responseVo=orderService.submitOrder(submitVo);
            Integer code = responseVo.getCode();
            //如果下单成功，去支付页面
            if (code==0){
                model.addAttribute("order", responseVo.getOrder());
                return "pay";
            //如果下单失败，回到 结算页
            }else {
                //在RedirectAttributes存入一个msg，表明校验失败的原因
                String msg = "下单失败;";
                switch (code) {
                    case 1:
                        msg += "防重令牌校验失败";
                        break;
                    case 2:
                        msg += "商品价格发生变化，请确认后再次提交";
                        break;
                }
                attributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        }catch (Exception e){
            if (e instanceof NoStockException){
                String msg = "下单失败，商品无库存";
                attributes.addFlashAttribute("msg", msg);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }

    /**
     * 获取当前用户的所有订单
     * @return
     */
    @RequestMapping("/memberOrder.html")
    public String memberOrder(@RequestParam(value = "pageNum",required = false,defaultValue = "0") Integer pageNum,
                              Model model){
        Map<String, Object> params = new HashMap<>();
        params.put("page", pageNum.toString());
        PageUtils page = orderService.getMemberOrderPage(params);
        model.addAttribute("pageUtil", page);
        return "list";
    }

}
