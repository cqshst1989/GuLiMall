package com.taoji666.gulimall.ware.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@EnableRabbit
@Configuration
public class MyRabbitmqConfig {

    /*
    * 使用JSON序列化机制，进行消息转换
    *
    * */
    @Bean
    public MessageConverter messageConverter() {
        //在容器中导入Json的消息转换器
        return new Jackson2JsonMessageConverter();
    }

    /*
    * 创建一个交换机
    * */
    @Bean
    public Exchange stockEventExchange() {
        return new TopicExchange("stock-event-exchange", true, false);
    }

    /**
     * 创建一个延迟队列
     *
     * 配置死信路由：信息死了以后给哪个交换机
     * 配置路由键
     * 配置消息过期时间
     *
     * 信息生产者会先将信息传到 这个队列，等消息死后，按照这里的配置，去stock-event-exchange交换机，再按照stock.release路由键
     * 去stockReleaseStockQueue，去了以后，就被 listener包下的StockReleaseListener 监听了
     * 由于死信过来的，就说明TTl时间内，你都没支付，我就要解锁库存 handleStockLockedRelease方法
     */
    @Bean
    public Queue stockDelayQueue() {
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "stock-event-exchange"); //信死以后，消息去的交换机
        arguments.put("x-dead-letter-routing-key", "stock.release"); //信死以后，用的路由键
        // 消息过期时间 2分钟
        arguments.put("x-message-ttl", 120000);
        return new Queue("stock.delay.queue", true, false, false, arguments);
    }

    /**
     * 普通队列，用于解锁库存
     * @return
     */
    @Bean
    public Queue stockReleaseStockQueue() {
        return new Queue("stock.release.stock.queue", true, false, false, null);
    }


    /**
     * 创建绑定关系：
     * 交换机和延迟队列绑定
     * @return
     *
     * 消息生产者的信息发送过来，rabbitTemplate.convertAndSend(交换机名字,路由键,javaBean）
     * javaBean里面就封装要发送的消息
     */
    @Bean
    public Binding stockLockedBinding() {
        return new Binding("stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",
                null);
    }

    /**
     * 创建绑定关系：
     * 交换机和普通队列绑定
     * @return
     */
    @Bean
    public Binding stockReleaseBinding() {
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.release.#",
                null);
    }

}
