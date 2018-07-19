package com.jas.seckill.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.jas.seckill.Const.Constants;
import com.jas.seckill.model.Goods;
import com.jas.seckill.model.ResponseMessage;
import com.jas.seckill.model.User;
import com.jas.seckill.service.SeckillService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ClassName:SeckillController
 * Package:com.jas.seckill.controller
 * Descrip:
 *
 * @Date:2018/7/16 下午8:22
 * @Author:jas
 */

@Controller
public class SeckillController {

    @Reference
    private SeckillService seckillService;

    /**
     * 查询所有商品
     * @param model
     * @return
     */
    @RequestMapping("/seckill/goods")
    public String showAllGoods(Model model){

        List<Goods> goodsList = seckillService.queryAllGoods();
        model.addAttribute("goodsList" , goodsList);

        return "goods";
    }

    /**
     * 查询单个商品细节
     * @param model
     * @param goodsId
     * @param session
     * @return
     */
    @RequestMapping("/seckill/goods/{id}")
    public String showGoods(Model model,
                            HttpSession session,
                            @PathVariable(value = "id") Integer goodsId ){
        //模拟用户登陆
        User user = new User();
        user.setUid("1");
        user.setUname("li");
        session.setAttribute(Constants.CONST_USER , user);

        Goods goods = seckillService.queryGoods(goodsId);

//        当前时间用来判断藐视是否开始
        model.addAttribute("systemTime" , System.currentTimeMillis());
        model.addAttribute("goods" , goods);
        return "goodsDetail";
    }

    /**
     * 通过id取出秒杀商品的唯一id
     * @param goodsId
     * @return
     */
    @PostMapping("/seckill/randomName/{id}")
    public @ResponseBody ResponseMessage getRandomName(@PathVariable(value = "id") Integer goodsId){
        //1.先要判断是否在秒杀期间
        Goods goods = seckillService.queryGoods(goodsId);
        long systemTime = System.currentTimeMillis();
        long startTime = goods.getStarttime().getTime();
        long endTime = goods.getEndtime().getTime();

        ResponseMessage responseMessage = new ResponseMessage();
        //如果没到秒杀时间
        if(systemTime < startTime){
            responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
            responseMessage.setErrorMessage("秒杀尚未开始");

        //若超过了秒杀时间
        }else if(systemTime > endTime){
            responseMessage.setErrorCode(Constants.ERROR_CODE_ZERO);
            responseMessage.setErrorMessage("秒杀已经结束");

        //若在秒杀时间范围内
        }else{
            //可以获取秒杀商品的唯一id
            responseMessage.setErrorCode(Constants.ERROR_CODE_ONE);
            responseMessage.setErrorMessage("获取成功");
            responseMessage.setResponseObj(goods.getRandomname());
        }

        return responseMessage;

    }

    /**
     * 执行秒杀商品的业务
     * @param session
     * @param goodsId
     * @param randomName
     * @return
     */
    @PostMapping("/seckill/start/{id}/{randomName}")
    public @ResponseBody ResponseMessage startSeckill(HttpSession session,
                                                      @PathVariable(value = "id") Integer goodsId,
                                                      @PathVariable(value = "randomName") String randomName){
        ResponseMessage responseMessage = new ResponseMessage();
        //获取登陆的用户
        User user = (User) session.getAttribute(Constants.CONST_USER);

//        ExecutorService pools = Executors.newFixedThreadPool(8);
//        for(int i =0 ; i <= 100 ; i++){
//            for (int j = 1 ; j <= 600 ; j++){
//                User users = new User();
//                users.setUid(String.valueOf(i));
//
//                pools.submit(new Runnable() {
//                    @Override
//                    public void run() {
//                        ResponseMessage responseMessage = seckillService.seckillGoods(goodsId , randomName , users.getUid());
//                        System.out.println(responseMessage.getErrorMessage());
//                    }
//                });
//            }
//        }

        //调用service来完成秒杀业务的逻辑操作
        responseMessage = seckillService.seckillGoods(goodsId , randomName , user.getUid());

        return responseMessage;
    }

    /**
     * 向redis中查询最终的订单结果
     * @param goodsId
     * @param session
     * @return
     */
    @PostMapping("/seckill/query/{id}")
    public @ResponseBody ResponseMessage queryOrders(@PathVariable(value = "id") Integer goodsId,
                                                     HttpSession session){
        User user = (User) session.getAttribute(Constants.CONST_USER);

        ResponseMessage responseMessage = seckillService.queryOrders(goodsId , user.getUid());

        return responseMessage;
    }

}
