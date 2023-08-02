package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.OrderDetail;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/2 14:37
 */
public interface OrderDetailService extends IService<OrderDetail> {
    /**
     * 根据订单号查询订单明细
     * @param orderId
     * @return
     */
    List<OrderDetail> orderDetails(Long orderId);
}
