package com.jas.seckill.service;

import com.jas.seckill.model.Goods;
import com.jas.seckill.model.Orders;
import com.jas.seckill.model.ResponseMessage;

import java.util.List;

/**
 * ClassName:Seckill
 * Package:com.jas.seckill.service
 * Descrip:
 *
 * @Date:2018/7/16 下午8:18
 * @Author:jas
 */
public interface SeckillService {
    /**
     * 查询所有商品的信息
     */
    public List<Goods> queryAllGoods();

    /**
     * 查询指定商品信息
     * @param goodsId
     * @return
     */
    public Goods queryGoods(Integer goodsId);

    /**
     * 处理具体的秒杀业务逻辑
     * @param goodsId
     * @param randomName
     * @param uid
     * @return
     */
    ResponseMessage seckillGoods(Integer goodsId, String randomName, String uid);

    /**
     * 进行下单操作
     * @param orders
     */
    void doOrders(Orders orders) throws Exception;

    /**
     * 从redis中查询最终的下单结果
     * @param goodsId
     * @param uid
     * @return
     */
    ResponseMessage queryOrders(Integer goodsId, String uid);

    /**
     * 处理接收消息和创建订单时的异常订单
     */
    void exceptionHandler(Orders orders);

    /**
     * 同步redis中的数据到数据库中
     * @param goods
     */
    void storeMysql(Goods goods);
}
