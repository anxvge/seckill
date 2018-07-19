package com.jas.seckill.task;

import com.jas.seckill.Const.Constants;
import com.jas.seckill.model.Goods;
import com.jas.seckill.redis.RedisUtil;
import com.jas.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import redis.clients.jedis.Jedis;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ClassName:RedisTask
 * Package:com.jas.seckill.task
 * Descrip:
 *
 * @Date:2018/7/17 下午9:27
 * @Author:jas
 */

@Configuration
@EnableScheduling //开启定时配置任务
public class RedisTask {

    @Autowired
    private SeckillService seckillService;

    //若redis中没有数据库中的商品库存,则从数据库中取出
    @Scheduled(cron = "0/5 * * * * *")  //每隔5秒进行一次操作
    public void storeRedis(){
        System.out.println("初始化商品库存中");

        //从数据库中查询商品库存加入redis中
        List<Goods>  list = seckillService.queryAllGoods();

        Jedis jedis = RedisUtil.getPool().getResource();
        try{
            for(Goods goods : list){
                //查出商品的库存
                Integer store = goods.getStore();

                //setnx方法是当key不存在时创建key并赋值,当key存在时就不操作
                jedis.setnx(Constants.REDIS_STORE + ":" + goods.getId() , String.valueOf(store));

            }
        }finally{
            if(null != jedis){
                jedis.close();
            }
        }

//        System.out.println("开始进行定时任务了");
    }

    //慢慢地将redis中的库存再同步到数据库中
    @Scheduled(cron = "0/5 * * * * *")
    public void storeMysql(){

        System.out.println("同步商品库存中");
        Jedis jedis = RedisUtil.getPool().getResource();
        try{
            //从redis中取出所有的store相关的key
            Set<String> keys = jedis.keys(Constants.REDIS_STORE + "*");

            Iterator iterator = keys.iterator();
            while(iterator.hasNext()){
                String key = (String) iterator.next();
                Integer goodsId = Integer.valueOf(key.split(":")[1]);
                Integer store = Integer.valueOf(jedis.get(key));
                Goods goods = new Goods();
                goods.setStore(store);
                goods.setId(goodsId);

                seckillService.storeMysql(goods);
            }

        }finally{
            if(null != jedis){
                jedis.close();
            }
        }
    }
}
