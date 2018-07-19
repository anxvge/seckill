package com.jas.seckill.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.Properties;

/**
 * ClassName:RedisUtil
 * Package:com.jas.seckill.redis
 * Descrip:
 *
 * @Date:2018/7/17 下午9:01
 * @Author:jas
 */
//自定义redis连接池的操作
public class RedisUtil {

    public static JedisPool pool = null;

    private static String host;

    private static int port;

    private static String password;

    static{
        Properties properties = new Properties();
        try {
            properties.load(RedisUtil.class.getClassLoader().getResourceAsStream("application.properties"));
            host = properties.getProperty("seckill.redis.host");
            port = Integer.parseInt(properties.getProperty("seckill.redis.port"));
            password = properties.getProperty("seckill.redis.password");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private RedisUtil(){}

    public static JedisPool getPool(){
        //由于是高并发下,防止二次击穿,采用双重验证加锁
        if(pool == null){
            synchronized(RedisUtil.class){
                if(pool == null){
                    JedisPoolConfig config = new JedisPoolConfig();
                    config.setMaxTotal(1000);//最大连接数
                    config.setMaxIdle(32);//最大空闲连接数
                    config.setMaxWaitMillis(90*1000);//获取连接时的最大等待毫秒数
                    config.setTestOnBorrow(true);//在获取连接的时候检查连接有效性

                    pool = new JedisPool(config , host , port , 15000 ,  password );
                }
            }
        }

        return pool;
    }

    public static void main(String[] args) {
        Jedis jedis = RedisUtil.getPool().getResource();
        System.out.println(jedis);

    }

}
