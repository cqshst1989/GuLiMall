package com.taoji666.gulimall.order.web;

import com.taoji666.gulimall.order.entity.OrderEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
public class HelloController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/testSubmit")
    public String testSubmitOrder() {
        //订单下单成功
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(UUID.randomUUID().toString());
        orderEntity.setModifyTime(new Date());

        /*
        * 传给配置好的交换机，交换机会将信息送去配置好的死信队列，死信队列会再把它push到普通队列
        *
        * 形参是 交换机 和 路由键
        * */
        rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",orderEntity);
        return "ok"; //这里就是响应ok，不是跳去页面@ResponseBody
    }
}