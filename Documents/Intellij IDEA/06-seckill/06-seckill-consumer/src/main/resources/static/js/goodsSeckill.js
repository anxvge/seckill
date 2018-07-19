//js模块化编程

var goodsSeckill = {

    //默认的项目路径
    contextPath : "",

    //轮询的id值
    intervalNum : "",

    //配置URL属性
    url : {
         getRandom : function () {
             return goodsSeckill.contextPath + "/seckill/randomName/";
         },

         seckill : function () {
             return goodsSeckill.contextPath + "/seckill/start/";
         },

         orders : function () {
             return goodsSeckill.contextPath + "/seckill/query/";
         }
    },

    func : {
        //初始化秒杀页面的秒杀按钮
        initSeckillButton : function (id , startTime , endTime , systemTime) {
            //判断当前时间是否在秒杀时间之内
            //若没到秒杀时间,进行显示倒计时操作
            if(systemTime < startTime){
                var finishTime = new Date(startTime + 1000);

                $("#seckillButton").countdown(finishTime , function (event) {
                    //初始化要显示的格式
                    var format = event.strftime('距秒杀开始还有: %D天 %H时 %M分 %S秒');
                    $("#seckillButton").html("<span>"+format+"</span>");
                }).on('finish.countdown' , function () {
                    goodsSeckill.func.startSeckill(id);
                });

            //若超过了秒杀时间
            }else if(systemTime > endTime){
                $("#seckillButton").html("<span style='color:red;'>秒杀活动已经结束</span>");

            //若在秒杀时间范围内
            }else{
                //可以进行秒杀操作
                goodsSeckill.func.startSeckill(id);
            }

        },

        //进行秒杀的开始操作
        startSeckill : function (id) {
            //先要从后台查询去除秒杀商品的唯一标识,防止利用页面源代码提前秒杀
            $.ajax({
                url:goodsSeckill.url.getRandom()+ id,
                dataType:"json",
                type:"post",
                success:function (responseMessage) {
                    if(responseMessage.errorCode == 0){
                        //获取失败
                        alert(responseMessage.errorMessage);
                    }else{
                        //获取成功,展示页面的秒杀按钮
                        $("#seckillButton").html("<button id='seckillClick'>点击秒杀</button>");

                        var randomName = responseMessage.responseObj;
                        $("#seckillClick").click(function(){
                            goodsSeckill.func.clickSeckill(id , randomName);
                        })
                    }

                }
            })
        },

        //开始秒杀
        clickSeckill : function (id , randomName) {
            $.ajax({
              url:goodsSeckill.url.seckill() + id + "/" + randomName,
              type:"post",
              dataType:"json",
              success:function (responseMessage) {
                  if(responseMessage.errorCode == 0){
                      //获取失败
                      $("#seckillButton").html(responseMessage.errorMessage);
                  }else{
                      //获取成功,展示返回的信息
                      $("#seckillButton").html(responseMessage.errorMessage);
                      
                      //由于只是中间结果,还需向数据库中查询最终的下单结果
                      //再向服务器发送请求,取得下单后的最终结果
                      goodsSeckill.intervalNum = window.setInterval(function () {
                          goodsSeckill.func.queryOrders(id)
                      } , 1000)
                      //每一秒轮询一次
                  }
              }
            })

        },
        
        queryOrders : function (id) {
            $.ajax({
                url:goodsSeckill.url.orders()+id,
                type:"post",
                dataType:"json",
                success:function (responseMessage) {
                    if(responseMessage.errorCode == 0){
                        //说明下单失败
                        $("#seckillButton").html(responseMessage.errorMessage);

                        //查询出结果后,结束轮询
                        window.clearInterval(goodsSeckill.intervalNum);
                    }else if(responseMessage.errorCode == 1){
                        //下单成功
                        $("#seckillButton").html(responseMessage.errorMessage);

                        //查询出结果后,结束轮询
                        window.clearInterval(goodsSeckill.intervalNum);
                    }

                    //由于消息可能为空,需要进行轮询,直到查询到订单结果为止
                }
            })
            
        }
    }
}
