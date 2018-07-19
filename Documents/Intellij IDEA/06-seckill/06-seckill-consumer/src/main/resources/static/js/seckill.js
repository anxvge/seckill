//JS模块化的开发方式

var seckillObject = {

    func : {

        //秒杀详情页秒杀时间初始化
        initGoods : function (nowTime, startTime, endTime) {
            //商品秒杀开始时间
            //商品秒杀结束时间
            //当前系统时间
            if (nowTime < startTime) {
                //秒杀未开始
                alert(1);
            } else if (nowTime > endTime) {
                //秒杀已结束
                alert(2);
            } else {
                //秒杀已开始 / 秒杀正在进行
                alert(3);
            }
        },

        initGoods2 : function () {
            alert(123321);
        }
    }
}