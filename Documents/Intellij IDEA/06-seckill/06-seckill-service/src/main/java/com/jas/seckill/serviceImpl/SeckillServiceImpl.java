package com.jas.seckill.serviceImpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONObject;
import com.jas.seckill.Const.Constants;
import com.jas.seckill.mapper.GoodsMapper;
import com.jas.seckill.mapper.OrdersMapper;
import com.jas.seckill.model.Goods;
import com.jas.seckill.model.Orders;
import com.jas.seckill.model.ResponseMessage;
import com.jas.seckill.redis.RedisUtil;
import com.jas.seckill.service.SeckillService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * ClassName:SeckillService
 * Package:com.jas.seckill.serviceImpl
 * Descrip:
 *
 * @Date:2018/7/16 下午8:37
 * @Author:jas
 */
@Component
@Service(timeout = 15000)
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Override
    public List<Goods> queryAllGoods() {
        return goodsMapper.selectAllGoods();
    }

    @Override
    public Goods queryGoods(Integer goodsId) {

        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);

        return goods;
    }

    @Override
    public ResponseMessage seckillGoods(Integer goodsId, String randomName, String uid) {
        //具体的秒杀业务逻辑
        ResponseMessage responseMessage = new ResponseMessage();
        //1.先判断是否在秒杀的时间范围内
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        long systemTime = System.currentTimeMillis();
        long startTime = goods.getStarttime().getTime();
        long endTime = goods.getEndtime().getTime();
        //如果没到秒杀时间
        if(systemTime < startTime){
            responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
            responseMessage.setErrorMessage("秒杀尚未开始");

            //若超过了秒杀时间
        }else if(systemTime > endTime){
            responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
            responseMessage.setErrorMessage("秒杀已经结束");

            //若在秒杀时间范围内,进行下一步操作
        }else{
            //2.判断商品是否已经卖光
            //由于是高并发的秒杀项目,所以如果通过数据库来查询的话,数据库会承受不住压力,所以使用redis来同步商品的库存
            //而且由springboot配置的redis并不好用,所以靠自己搭建一个redis的连接池来使用redis
            Jedis jedis = RedisUtil.getPool().getResource();
            try{
                //利用Task计时任务定时从数据库中读取商品的库存

                //导入一个apache下的常用工具类,判断是否为空
                Integer store = StringUtils.isEmpty(jedis.get(Constants.REDIS_STORE+":"+goodsId))? 0 : Integer.valueOf(jedis.get(Constants.REDIS_STORE+":"+goodsId));

                if(store <= 0){
                    responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
                    responseMessage.setErrorMessage("对不起,商品已经卖完了");
                }else{
                    //3.进行限流
                    //只允许所秒杀商品的100倍的流量进入,其余的进行拦截操作
//                        //为了防止高并发,可以先加入,再进行判断吗?不可以,这样子有可能会导致所有人都进不来
//                        long  flow = jedis.rpush(Constants.REDIS_FLOW+":"+goodsId , uid);
//                        if(flow > store*100){
//                            responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
//                            responseMessage.setErrorMessage("对不起,网络拥挤,请稍后再试");
//                            jedis.rpop(Constants.REDIS_FLOW+":"+goodsId);
//                        }
//                      Long flow = jedis.llen(Constants.REDIS_FLOW+":"+goodsId);
                    Long flowLimit = store*100L;
                    //如果大于了限定流量
//                        if(flow >= flowLimit){
//                            responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
//                            responseMessage.setErrorMessage("对不起,网络拥挤,请稍后再试");
//                        }else{
//                            //如果小于
//                            if(jedis.llen(Constants.REDIS_FLOW+":"+goodsId) < flowLimit){
//                                //则往redis中添加一个流量标识
//                                jedis.rpush(Constants.REDIS_FLOW+":"+goodsId , uid);
//                            }else{
//                                responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
//                                responseMessage.setErrorMessage("对不起,网络拥挤,请稍后再试");
//                            }
//                        }
                    if(jedis.llen(Constants.REDIS_FLOW+":"+goodsId) < flowLimit){
                        //则往redis中添加一个流量标识
                        jedis.rpush(Constants.REDIS_FLOW+":"+goodsId , uid);

                        //4.判断用户是否已经买过了
                        //如果用户买过了.会在redis中存储一个标识

                        //TODO 使用自增字段,用户的第一个请求字段值为0,若不为0,说明不是第一次请求,拒绝该请求,相当于是认为已经购买过
                        Long sign = jedis.incr(Constants.REDIS_PREFIX+":"+uid+":"+goodsId);

                        System.out.println(sign);
                        if(1 != sign){
                            responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
                            responseMessage.setErrorMessage("对不起,你的订单正在处理或者你已经购买过该商品了");

                            jedis.rpop(Constants.REDIS_FLOW+":"+goodsId);

                        }else{

                            //5.下单操作
                            //先要把库存减一,然后再进行下单,这些在高并发的情况下,不建议使用数据库的乐观锁处理,建议使用redis
                            //因为redis是单线程的,可以把多线程下的请求按照顺序一个一个进行处理,变成串行化,避免出现超卖现象

                            //一次减少一件库存
                            Long leftStore = jedis.decrBy(Constants.REDIS_STORE+":"+goodsId , 1);
                            if(leftStore >= 0){
                                //说明可以下单
                                //下单由于是在高并发的情况下,如果直接由数据库进行大量同时的订单的存储负荷过大,有可能会无响应,因此采用异步的方式
                                //把一个高峰流量削峰,变成平缓流量,即先异步地用mq将订单信息发布,再从后台消息接收者接收消息存入数据库

                                //配置activeMQ,导入依赖,配置application.properties配置文件
                                //自动导入jmsTemplate

                                Orders orders = new Orders();
                                //创建订单
                                orders.setBuynum(1);
                                orders.setBuyprice(goods.getPrice());
                                orders.setCreatetime(new Date());
                                orders.setGoodsid(goodsId);
                                orders.setUid(Integer.valueOf(uid));
                                orders.setStatus(1); //1待支付
                                orders.setOrdermoney(goods.getPrice().multiply(new BigDecimal(1)));

                                String ordersObject = JSONObject.toJSONString(orders);

                                jmsTemplate.send(new MessageCreator() {
                                    @Override
                                    public Message createMessage(Session session) throws JMSException {
                                        return session.createTextMessage(ordersObject);
                                    }
                                });

                                System.out.println("进入了下单这一步");

                                //由于是异步发送消息,所以正式的下单结果需要再次查询才能得到,现在只能得到一个下单请求成功的中间结果
                                //先暂时把这个返回给前段,再由前端查询后台订单消息返回正式的结果

                                responseMessage.setErrorCode(Constants.ERROR_CODE_ONE);
                                responseMessage.setErrorMessage("恭喜,下单请求发送成功,请稍等...");


                            }else{
                                //值会被减成负数,我们可以为了美观再加上去,虽然不影响程序运行
                                jedis.incrBy(Constants.REDIS_STORE+":"+goodsId , 1);
                                responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
                                responseMessage.setErrorMessage("对不起,商品已经卖光了");

                                //TODO 去掉用户的购买标识
                                jedis.del(Constants.REDIS_PREFIX+":"+uid+":"+goodsId);
                                jedis.rpop(Constants.REDIS_FLOW+":"+goodsId);
                            }


                        }
                    }else{
                        responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
                        responseMessage.setErrorMessage("对不起,网络拥挤,请稍后再试");
                    }

                }
            }finally{
                if(null != jedis){
                    jedis.close();
                }
            }

        }
        return responseMessage;
    }

    /**
     * 进行下单操作
     * 还要对限流以及购买标识进行处理
     * 可以将下单后的情况以及订单保存到redis中
     * @param orders
     */
    @Override
    @Transactional  //加入事务管理,防止下单成功后由于其他原因抛出异常,导致订单存在用户却无法得到通知
    public void doOrders(Orders orders) throws Exception {
        ResponseMessage responseMessage = new ResponseMessage();
        Jedis jedis = RedisUtil.getPool().getResource();

        int count = ordersMapper.insertSelective(orders);

        //先下单
        try{
            //判断是否下单成功
            if(count == 1){
               //下单成功,为该用户增加购买标识
//                jedis.set(Constants.REDIS_PREFIX+":"+orders.getUid()+":"+orders.getGoodsid() , String.valueOf(orders.getGoodsid()));
//                //jedis.incrBy(Constants.REDIS_PREFIX+":"+orders.getUid()+":"+orders.getGoodsid() , 1);

                System.out.println("下单成功");

                //保存成功的订单信息
                responseMessage.setErrorCode(Constants.ERROR_CODE_ONE);
                responseMessage.setErrorMessage("恭喜,下单成功,请立即支付");
                responseMessage.setResponseObj(orders);
                String message = JSONObject.toJSONString(responseMessage);
                jedis.set(Constants.REDIS_ORDER+":"+orders.getGoodsid()+":"+orders.getUid() , message);

                //将限制的流量数减一
                jedis.rpop(Constants.REDIS_FLOW+":"+orders.getGoodsid());
            }else {
                //下单失败,购买标识不需要处理
                //TODO 订单失败,去掉用户的购买标识
                jedis.del(Constants.REDIS_PREFIX+":"+orders.getUid()+":"+orders.getGoodsid());

                //处理失败的订单信息
                responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
                responseMessage.setErrorMessage("对不起,订单创建失败,请重新尝试");
                responseMessage.setResponseObj(orders);
                String message = JSONObject.toJSONString(responseMessage);
                jedis.set(Constants.REDIS_ORDER + ":" + orders.getGoodsid() + ":" + orders.getUid(), message);

                //由于订单创建失败的缘故,需要库存重新加一
                jedis.incrBy(Constants.REDIS_STORE+":"+orders.getGoodsid() , 1);

                //将限制的流量数减一
                jedis.rpop(Constants.REDIS_FLOW + ":" + orders.getGoodsid());
            }

        }finally{
            if(null != jedis){
                jedis.close();
            }
        }


    }

    @Override
    public ResponseMessage queryOrders(Integer goodsId, String uid) {
        Jedis jedis = RedisUtil.getPool().getResource();
        String result = jedis.get(Constants.REDIS_ORDER+":"+goodsId+":"+uid);

        try{
            //判断redis中是否已经获得了该下单信息,若没有,则返回一个空的对象
            ResponseMessage responseMessage = StringUtils.isEmpty(result)? new ResponseMessage() : JSONObject.parseObject(result , ResponseMessage.class);

            return responseMessage;
        }finally{
            if(null != jedis){
                jedis.close();
            }
        }
    }

    @Override
    public void exceptionHandler(Orders orders) {
        ResponseMessage responseMessage = new ResponseMessage();
        Jedis jedis = RedisUtil.getPool().getResource();
        try{
            responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
            responseMessage.setErrorMessage("对不起,订单创建失败,请重新尝试");
            responseMessage.setResponseObj(orders);
            String message = JSONObject.toJSONString(responseMessage);
            jedis.set(Constants.REDIS_ORDER + ":" + orders.getGoodsid() + ":" + orders.getUid(), message);

            //TODO 订单失败,去掉用户的购买标识
            jedis.del(Constants.REDIS_PREFIX+":"+orders.getUid()+":"+orders.getGoodsid());

            //由于订单创建失败的缘故,需要库存重新加一
            jedis.incrBy(Constants.REDIS_STORE+":"+orders.getGoodsid() , 1);

            //将限制的流量数减一
            jedis.rpop(Constants.REDIS_FLOW + ":" + orders.getGoodsid());

        }finally{
            if(null != jedis){
                jedis.close();
            }
        }
    }

    @Override
    public void storeMysql(Goods goods) {
        goodsMapper.updateByPrimaryKeySelective(goods);
    }
}
