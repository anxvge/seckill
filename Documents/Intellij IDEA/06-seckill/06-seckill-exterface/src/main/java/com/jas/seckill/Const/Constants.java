package com.jas.seckill.Const;

import java.io.Serializable;

/**
 * ClassName:Constants
 * Package:com.jas.seckill.Const
 * Descrip:一个常量类
 *
 * @Date:2018/7/17 下午7:58
 * @Author:jas
 */
public class Constants implements Serializable {
    /**
     * 错误码为0,表示处理失败
     */
   public static final Integer ERROR_CODE_ZERO = 0;

    /**
     * 错误码为1,表示处理成功
     */
   public static final Integer ERROR_CODE_ONE = 1;

    /**
     * 保存在session中的用户
     */
   public static final String CONST_USER = "user";

    /**
     * redis存储商品信息的常量前缀
     */
    public static final String REDIS_PREFIX = "GoodsRedis";

    /**
     * redis存储商品库存的常量前缀
     */
    public static final String REDIS_STORE = "GoodsStore";

    /**
     * redis存储网站限流的常量前缀
     */
    public static final String REDIS_FLOW = "WebFlowLimit";

    /**
     * redis存储秒杀的订单信息
     */
    public static final String REDIS_ORDER = "GoodsOrders";

}
