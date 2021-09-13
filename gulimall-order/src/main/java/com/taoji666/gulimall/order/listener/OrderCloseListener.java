package com.taoji666.gulimall.order.listener;

import com.rabbitmq.client.Channel;
import com.taoji666.gulimall.order.entity.OrderEntity;
import com.taoji666.gulimall.order.service.OrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


/*
* 完成定时关单：订单TTL时间没支付，关闭该订单
* */
@Component
@RabbitListener(queues = {"order.release.order.queue"})
public class OrderCloseListener {

    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void listener(OrderEntity orderEntity, Message message, Channel channel) throws IOException {
        System.out.println("收到过期的订单信息，准备关闭订单" + orderEntity.getOrderSn());
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            orderService.closeOrder(orderEntity); //操作关闭order
            channel.basicAck(deliveryTag,false); //手动确认后，就会自动删除队列中的消息
        } catch (Exception e){
            //拒绝，重新回队列
            channel.basicReject(deliveryTag,true);
        }

    }
}
