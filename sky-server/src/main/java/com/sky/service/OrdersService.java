package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/2 14:12
 */
public interface OrdersService extends IService<Orders> {
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 根据订单号和用户 id 查询订单
     * @param orderNumber
     * @return
     */
    Orders getOrdersByNumber(String orderNumber);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO);

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 历史订单分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单 id 查询订单详情
     * @param id
     * @return
     */
    OrderVO details(Long id);

    /**
     * 删除订单
     * @param id
     */
    void cancelById(Long id);

    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);
}
