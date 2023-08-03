package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.sky.entity.User;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/3 19:07
 */
@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {
    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.lt(Orders::getOrderTime, end).gt(Orders::getOrderTime, begin);
        Integer totalOrderCount = ordersMapper.selectCount(lqw);    // 总订单数
        LambdaQueryWrapper<Orders> lqw1 = new LambdaQueryWrapper<>();
        lqw1.lt(Orders::getOrderTime, end).gt(Orders::getOrderTime, begin).eq(Orders::getStatus, Orders.COMPLETED);
        Integer validOrderCount = ordersMapper.selectCount(lqw1);   // 有效订单数
        LocalDate date = end.toLocalDate();
        Double turnover = ordersMapper.sumAmountInOneDay(Orders.COMPLETED, date);   // 当天总金额
        Double unitPrice = 0.0; // 平均单价
        Double orderCompletionRate = 0.0;   // 订单完成率
        if(totalOrderCount != 0 && validOrderCount != 0){
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }
        // 新增用户数
        LambdaQueryWrapper<User> lqw2 = new LambdaQueryWrapper<>();
        lqw2.like(User::getCreateTime, date);
        Integer newUser = userMapper.selectCount(lqw2);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUser)
                .build();
    }

    /**
     * 查询订单管理数据
     *
     * @return
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        LocalDate date = LocalDateTime.now().toLocalDate();
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Orders::getStatus, Orders.TO_BE_CONFIRMED).like(Orders::getOrderTime, date);
        Integer waitingOrders = ordersMapper.selectCount(lqw);  // 待接单
        lqw.clear();
        lqw.eq(Orders::getStatus, Orders.CONFIRMED).like(Orders::getOrderTime, date);
        Integer deliveredOrders = ordersMapper.selectCount(lqw);    // 待派送
        lqw.clear();
        lqw.eq(Orders::getStatus, Orders.COMPLETED).like(Orders::getOrderTime, date);
        Integer completeOrders = ordersMapper.selectCount(lqw); // 已完成
        lqw.clear();
        lqw.eq(Orders::getStatus, Orders.CANCELLED).like(Orders::getOrderTime, date);
        Integer cancelledOrders = ordersMapper.selectCount(lqw);    // 已取消
        lqw.clear();
        lqw.like(Orders::getOrderTime, date);
        Integer allOrders = ordersMapper.selectCount(lqw);  // 全部订单

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completeOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    @Override
    public DishOverViewVO getDishOverView() {
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Dish::getStatus, StatusConstant.ENABLE);
        Integer sold = dishMapper.selectCount(lqw); // 起售的菜品数量
        lqw.clear();
        lqw.eq(Dish::getStatus, StatusConstant.DISABLE);
        Integer discontinued = dishMapper.selectCount(lqw); // 停售的菜品数量

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    @Override
    public SetmealOverViewVO getSetmealOverView() {
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Setmeal::getStatus, StatusConstant.ENABLE);
        Integer sold = setmealMapper.selectCount(lqw);  // 起售的套餐数量
        lqw.clear();
        lqw.eq(Setmeal::getStatus, StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.selectCount(lqw);  // 停售的套餐数量

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}
