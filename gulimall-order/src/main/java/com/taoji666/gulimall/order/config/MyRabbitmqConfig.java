package com.taoji666.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;


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



}
