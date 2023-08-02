package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.entity.OrderDetail;
import com.sky.mapper.OrderDetailMapper;
import com.sky.service.OrderDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/2 14:38
 */
@Service
@Slf4j
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    /**
     * 根据订单号查询订单明细
     * @param orderId
     * @return
     */
    @Override
    public List<OrderDetail> orderDetails(Long orderId) {
        LambdaQueryWrapper<OrderDetail> lqw = new LambdaQueryWrapper<>();
        lqw.eq(null != orderId, OrderDetail::getOrderId, orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(lqw);
        return orderDetailList;
    }
}
