package com.taoji666.gulimall.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;


/*
* RabbitMq 默认是 java序列化机制，存储的消息都是序列化后的一堆乱码
* 因此配置这个转换器，让其存储json
* */
@Configuration
public class MyRabbitmqConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter messageConverter() {
        //在容器中导入Json的消息转换器
        return new Jackson2JsonMessageConverter();
    }

    /*
    * 定制RabbitTemplate   主要配置
    * 1、消息从 生产者 到 消息代理服务器 成功后的回调信息
    * 2、消息从 消息代理服务器 到 队列 成功后的回调
    *
    * （这个不用写配置，直接在spring配置文件中写就好）
    * 3、消费端确认，保证每个消息被正确消费，此时才可以让 消息代理服务器 删除这个消息
    *  默认是：自动确认。即只要消息接收到，客户端就自动确认，服务端就会移除这个消息
    *  问题：
    *     我们收到很多消息，自动回复给服务器ack，但是只有一个消息`处理`成功，就宕机了。此时就会发生消息丢失
    * 解决办法：
    *     修改自动确认 为  手动确认：只要我们没有明确告诉MQ，消息 已被签收。没有Ack，消息就是一直是unacked状态，即使消费者宕机，消息也不会丢失
    * 并且重新变为ready状态，下次有新的 `消费者` 连接进来，就发给他
    * spring配置文件中
    * listener:
        simple:
          acknowledge-mode: manual
    *
    * 1、消息服务器收到消息就立刻回调的方法，该方法传入好了 id，ack，和失败原因
    * 使用该方法，首先要在spring配置文件中配置 publisher-confirms: true
    *
    * */
    @PostConstruct //MyRabbitmqConfig的对象创建完成后，就立刻执行该方法
    public void initRabbitTemplate(){
        //只要代理收到消息，本方法自动回调

        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback(){

            /**
            * 传入参数：
            * @param correlationData 当前消息的唯一ID
              @param            ack   消息是否成功收到，只要消息抵达 消息代理服务器，就为true
             *@param  cause           失败原因
             *
             * 1、做好消息确认机制（publisher、consumer【手动ack】）
             * 2、每一个发送的消息都在数据库做好，定期将失败消息再次发送
             * */
            @Override
            public void confirm(CorrelationData correlationData,boolean ack, String cause){
                System.out.println("confirm...correlationData["+correlationData+"]==>ack["+ack+"]==>cause["+cause+"]");
            }
        });
        /**
         * 2、消息从 消息代理服务器 到 队列 成功后的回调
         * 使用该方法，首先要在spring配置文件中配置 publisher-returns: true 和 template: mandatory: true
         *
         * 该方法触发时机：消息没有投递给指定的队列，才触发这个失败回调
         * @param message,  投递失败的消息详细信息
         * @param replyCode, 回复的状态码
         * @param replyText, 回复的文本内容
         * @param exchange,  当时这个消息发给哪个交换机
         * @param routineKey 当时这个消息用哪个路由键
         * */

        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routineKey) {
                System.out.println("Fail Message["+message+"]==>replyCode["+replyCode+"]==>exchange["+exchange+"]===>routineKey["+routineKey+"]");
            }
        });

    }

    /**
    *  创建2个延迟队列（Queue） 和 一个交换机（Exchange）  以及队列与交换机的2个绑定关系（Binding）
     *
     * 死信队列中的消息，将会被交换机重新push到普通队列
     * */


    /*
    * 交换机：
    * 由于两个队列都连接这个交换机，所以使用Topic交换机
    * */
    @Bean
    public Exchange orderEventExchange() {
        /**
         *   String name,  交换机的名字：order-event-exchange
         *   boolean durable,  持久化 true
         *   boolean autoDelete, 自动删除 false
         *   Map<String, Object> arguments， 不设置属性，就只需要一个普通交换机
         */
        return new TopicExchange("order-event-exchange", true, false);
    }

    /**
     * 死信队列DLX，dead-letter-exchange：有TTl时间，时间一到，消息就“死”了
     * 将队列中的“死”信息，重新push到另一个Exchange，该Exchange就是DLX
     *
     * 消息变成”死信“的原因
     * （1）消息被拒绝(basic.reject / basic.nack)，并且requeue = false
     * （2）消息TTL过期
     * （3）队列达到最大长度
     *
     *
     * 特别注意：
     * 一旦队列创建成功，哪怕参数写错了，在这里修改后，重新创建。并不会覆盖掉原来错误的
     * 想删除原来错误的队列，只能去兔子MQ 服务端，手动删除
     */
    @Bean
    public Queue orderDelayQueue() {
        /**
         Queue(String name,  队列名字：order.delay.queue
         boolean durable,  是否持久化  true
         boolean exclusive,  是否排他  false   如果你想创建一个只有自己可见的队列，即不允许其它用户访问,就可以设置为true
         boolean autoDelete, 是否自动删除  false
         Map<String, Object> arguments) 属性
         */

        //准备好队列的特殊属性：死信路由，死信路由键，消息过期时间
        HashMap<String, Object> arguments = new HashMap<>();
        //死信交换机
        arguments.put("x-dead-letter-exchange", "order-event-exchange");
        //死信路由键
        arguments.put("x-dead-letter-routing-key", "order.release.order");
        arguments.put("x-message-ttl", 60000); // 消息过期时间 1分钟
        return new Queue("order.delay.queue",true,false,false,arguments);
    }

    /**
     * 普通队列：就不需要设置队列属性了
     *
     * @return
     */
    @Bean
    public Queue orderReleaseQueue() {

        Queue queue = new Queue("order.release.order.queue", true, false, false);

        return queue;
    }

    /**
     * 创建订单的binding
     * 绑定死信队列 和 交换机
     */
    @Bean
    public Binding orderCreateBinding() {
        /**
         * String destination, 目的地（队列名或者交换机名字），这里当然是queue名字
         * DestinationType destinationType, 目的地类型（Queue or Exhcange）  这里当然是 Queue类型
         * String exchange,  交换机名字
         * String routingKey,  路由键
         * Map<String, Object> arguments  这里没有绑定关系属性设置，由于是全参构造器，就必须传null
         * */
        return new Binding("order.delay.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.create.order", null);
    }


    /*
    * 死信队列里面的消息，会被重新push到这个队列
    * */
    @Bean
    public Binding orderReleaseBinding() {
        return new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange", //交换机就一种
                "order.release.order",
                null);
    }
    /*
    * 订单释放，直接和库存释放进行绑定
    *
    * */
    @Bean
    public Binding orderReleaseOrderBinding() {
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.other.#",
                null);
    }

    /**
     * 商品秒杀队列
     * @return
     */
    @Bean
    public Queue orderSecKillOrrderQueue() {
        Queue queue = new Queue("order.seckill.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Binding orderSecKillOrrderQueueBinding() {
        //String destination, DestinationType destinationType, String exchange, String routingKey,
        // 			Map<String, Object> arguments
        Binding binding = new Binding(
                "order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",
                null);

        return binding;
    }



}
