package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Kuroko
 * @description 订单状态定时处理
 * @date 2023/8/3 14:52
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrdersMapper ordersMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟处理一次
    public void processTimeOutOrder(){
        log.info("定时处理超时订单:{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        // 到当前时间为止还未支付的订单都属于超时订单
        lqw.eq(Orders::getStatus, Orders.PENDING_PAYMENT);
        lqw.lt(Orders::getOrderTime, time);
        List<Orders> ordersList = ordersMapper.selectList(lqw);
        // 存在超时订单
        if(ordersList != null && ordersList.size() > 0){
            for(Orders order : ordersList){
                // 修改订单状态
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                ordersMapper.updateById(order);
            }
        }
    }

    /**
     * 处理一直处于派送中的订单
     * 这里的做法是每天凌晨一点进行一次处理，处理前一天仍在配送的订单。不是很严谨
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理一直处于派送中的订单:{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusHours(-1);
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS);
        lqw.lt(Orders::getOrderTime, time);
        List<Orders> ordersList = ordersMapper.selectList(lqw);
        if(ordersList != null && ordersList.size() > 0){
            for(Orders order : ordersList){
                order.setStatus(Orders.COMPLETED);
                //order.setDeliveryTime(LocalDateTime.now());
                ordersMapper.updateById(order);
            }
        }
    }
}
