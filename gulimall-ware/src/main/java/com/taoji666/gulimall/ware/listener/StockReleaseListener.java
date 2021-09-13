package com.taoji666.gulimall.ware.listener;

import com.rabbitmq.client.Channel;
import com.taoji666.common.to.mq.OrderTo;
import com.taoji666.common.to.mq.StockLockedTo;
import com.taoji666.gulimall.ware.service.WareSkuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RabbitListener(queues = {"stock.release.stock.queue"})
public class StockReleaseListener {

    @Autowired
    private WareSkuService wareSkuService;

    /*库存自动解锁
     * 1、查询数据库关于这个订单的锁定库存信息
     * 有：证明库存已经锁定成功
     *    解锁：根据订单情况
     *      （1) 没有这个订单，必须解锁
     *      （2）有这个订单，
     *           订单状态：已取消：解锁库存
     *                   没取消：不能解锁库存
     * 没有：库存锁定失败，库存回滚，这种情况无需解锁
     *
     * */

    @RabbitHandler  //yml配置文件中，配置成manual 手动接收，之后可以在这里使用channel来手动接收
    public void handleStockLockedRelease(StockLockedTo stockLockedTo, Message message, Channel channel) throws IOException {
        log.info("************************收到库存解锁的消息********************************");
        try {
            wareSkuService.unlock(stockLockedTo); //这个是干正事的，即解锁库存
            //已经解锁成功，即手动确认消息消费成功，消费成功后，自动删除队列中的消息了
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            //消息拒绝后，重新放入队列，让别人继续消费解锁
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

    @RabbitHandler
    public void handleStockLockedRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        log.info("************************从订单模块收到库存解锁的消息********************************");
        try {
            wareSkuService.unlock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }
}