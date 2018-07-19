package com.jas.seckill.activemq;

import com.alibaba.fastjson.JSONObject;
import com.jas.seckill.model.Orders;
import com.jas.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

/**
 * ClassName:AsyncListener
 * Package:com.jas.seckill.activemq
 * Descrip:
 *
 * @Date:2018/7/18 上午10:38
 * @Author:jas
 */
@Component
public class AsyncListener {

    @Autowired
    private SeckillService seckillService;

    @JmsListener(destination = "${spring.jms.template.default-destination}" , concurrency = "8")
    public void receiveMessage(Message message){
        Orders orders = null;

        if(message instanceof TextMessage){
            try {
                String ordersObject = ((TextMessage) message).getText();

                //将字符串转化为订单信息
                orders = JSONObject.parseObject(ordersObject , Orders.class);

                System.out.println("接收到消息了");
                //进行下单操作
                try {
                    seckillService.doOrders(orders);
                } catch (Exception e) {
                    seckillService.exceptionHandler(orders);
                }

            } catch (JMSException e) {
                seckillService.exceptionHandler(orders);
            }
        }

    }

}
