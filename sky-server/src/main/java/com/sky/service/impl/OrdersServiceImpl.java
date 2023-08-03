package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.OrdersMapper;
import com.sky.result.PageResult;
import com.sky.service.*;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.webSocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/2 14:13
 */
@Service
@Slf4j
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 异常情况处理:收货地址为空、超出配送范围、购物车为空等等
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        // 查询收获地址
        AddressBook address = addressBookService.getAddressById(addressBookId);
        if(address == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        // 查询用户购物车数据
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCart);
        if(shoppingCartList == null || shoppingCartList.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 构造订单数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        // 将收货人部分信息填入订单中
        orders.setPhone(address.getPhone());
        orders.setAddress(address.getDetail());
        orders.setConsignee(address.getConsignee());
        // 生成订单号
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setUserId(userId);
        // 订单舒适状态为待付款
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setOrderTime(LocalDateTime.now());
        // 像订单表插入一条数据
        ordersMapper.insert(orders);

        // 向订单明细表插入多条数据,购物车中每一条商品数据对应一条订单明细
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart cart : shoppingCartList){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailService.saveBatch(orderDetailList);

        // 下单后需要清空用户购物车数据
        shoppingCartService.cleanShoppingCart();

        // 封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 根据订单号和用户 id 查询订单
     * @param orderNumber
     * @return
     */
    @Override
    public Orders getOrdersByNumber(String orderNumber) {
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(orderNumber), Orders::getNumber, orderNumber);
        Orders orders = ordersMapper.selectOne(lqw);
        return orders;
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 获取当前用户
        Long userId = BaseContext.getCurrentId();
        User user = userService.getById(userId);
//        //调用微信支付接口，生成预支付交易单。需要企业用户开通服务才行，目前实现不了，看看大概就行
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );

        // 模拟订单支付
        JSONObject jsonObject = new JSONObject();
        if(jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")){
            throw new OrderBusinessException("该订单已支付");
        }

        // 封装返回数据
        OrderPaymentVO orderPaymentVO = jsonObject.toJavaObject(OrderPaymentVO.class);
        orderPaymentVO.setPackageStr(jsonObject.getString("package"));
        paySuccess(ordersPaymentDTO.getOrderNumber());
        return orderPaymentVO;
    }

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    @Override
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单信息
        Orders order = getOrdersByNumber(outTradeNo);
        // 修改订单状态
        order.setStatus(Orders.TO_BE_CONFIRMED);
        order.setPayStatus(Orders.PAID);
        order.setCheckoutTime(LocalDateTime.now());

        ordersMapper.updateById(order);

        // 通过 weSocket 向商家管理端推送消息
        Map map = new HashMap();
        map.put("type", 1); // 消息类型：1、来单提醒
        map.put("orderId", order.getId());
        map.put("content", "订单号: " + outTradeNo);
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 用户催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        Orders orders = ordersMapper.selectById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 基于 WebSocket 实现催单
        Map map = new HashMap();
        map.put("type", 2); // 消息类型：2、用户催单
        map.put("orderId", id);
        map.put("content", "订单号: " + orders.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 历史订单分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(null != userId, Orders::getUserId, userId);
        lqw.eq(null != ordersPageQueryDTO.getStatus(), Orders::getStatus, ordersPageQueryDTO.getStatus());
        Page<Orders> ordersPage = ordersMapper.selectPage(page, lqw);

        // 将查询出的数据封装入 OrderVO 返回
        List<OrderVO> list = new ArrayList<>();
        if(ordersPage != null && ordersPage.getTotal() > 0){
            for(Orders orders : ordersPage.getRecords()){
                // 订单 id
                Long id = orders.getId();
                // 查询订单明细
                List<OrderDetail> orderDetailList = orderDetailService.orderDetails(id);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            }
        }
        return new PageResult(ordersPage.getTotal(), list);
    }

    /**
     * 根据订单 id 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        Orders orders = ordersMapper.selectById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        // 查询订单详情
        List<OrderDetail> orderDetailList = orderDetailService.orderDetails(id);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 删除订单
     * @param id
     */
    @Override
    public void cancelById(Long id) {
        // 查询订单信息
        Orders orders = ordersMapper.selectById(id);
        // 检查订单是否存在
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 已完成的或者正在处理订单不能删除
        // 订单状态:1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if(orders.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 待接单的订单已经付款了，退单需要进行退款
        if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
//            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        ordersMapper.updateById(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        // 将原订单的商品重复一份添加到购物车中
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetailList = orderDetailService.orderDetails(id);

        //  将订单详情对象转为购物车对象重新添加一份到购物车中
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            // 将原订单的菜品信息重新复制到购物车中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartService.saveBatch(shoppingCartList);
    }

    /**
     * 管理端订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        /**
         * 输入订单号/手机号进行搜索，支持模糊搜索
         * 根据订单状态进行筛选
         * 下单时间进行时间筛选
         */
        lqw.like(StringUtils.isNotEmpty(ordersPageQueryDTO.getNumber()), Orders::getNumber, ordersPageQueryDTO.getNumber());
        lqw.like(StringUtils.isNotEmpty(ordersPageQueryDTO.getPhone()), Orders::getPhone, ordersPageQueryDTO.getPhone());
        lqw.eq(null != ordersPageQueryDTO.getStatus(), Orders::getStatus, ordersPageQueryDTO.getStatus());
        lqw.ge(null != ordersPageQueryDTO.getBeginTime(), Orders::getOrderTime, ordersPageQueryDTO.getBeginTime());
        lqw.le(null != ordersPageQueryDTO.getEndTime(), Orders::getDeliveryTime, ordersPageQueryDTO.getEndTime());
        lqw.orderByDesc(Orders::getOrderTime);
        Page<Orders> ordersPage = ordersMapper.selectPage(page, lqw);
        List<Orders> records = ordersPage.getRecords();
        // 将菜品信息封装成 OrderVO 返回
        List<OrderVO> orderVOList = new ArrayList<>();
        if(records != null && records.size() > 0){
            for(Orders orders : records){
                Long id = orders.getId();
                List<OrderDetail> orderDetailList = orderDetailService.orderDetails(id);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                String orderDishesStr = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishesStr);
                orderVOList.add(orderVO);
            }
        }
        return new PageResult<>(ordersPage.getTotal(), orderVOList);
    }

    /**
     * 根据订单 id 获取菜品信息字符串
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders){
        Long id = orders.getId();
        List<OrderDetail> orderDetailList = orderDetailService.orderDetails(id);
        // 将每一条订单菜品信息拼接成字符串,(菜品 * 数量)
        List<String> stringList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单的所有菜品信息拼接在一起
        return String.join("", stringList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        LambdaQueryWrapper<Orders> lqw1 = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Orders> lqw2 = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Orders> lqw3 = new LambdaQueryWrapper<>();
        // 根据状态分别查出待接单、待派送、派送中的订单数量
        lqw1.eq(Orders::getStatus, Orders.TO_BE_CONFIRMED);
        lqw2.eq(Orders::getStatus, Orders.CONFIRMED);
        lqw3.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfiremed = ordersMapper.selectCount(lqw1);
        Integer confirmed = ordersMapper.selectCount(lqw2);
        Integer deliveryInProgress = ordersMapper.selectCount(lqw3);

        // 将查询出的结果封装到 OrderStatisticsVO 中返回
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setToBeConfirmed(toBeConfiremed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Long id = ordersConfirmDTO.getId();
        Orders orders = ordersMapper.selectById(id);
        orders.setStatus(Orders.CONFIRMED);
        ordersMapper.updateById(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        /**
         * 商家拒单其实就是将订单状态修改为“已取消”
         * 只有订单处于“待接单”状态时可以执行拒单操作
         * 商家拒单时需要指定拒单原因
         * 商家拒单时，如果用户已经完成了支付，需要为用户退款
         */
        Long id = ordersRejectionDTO.getId();
        Orders orders = ordersMapper.selectById(id);
        // 只有订单处于“待接单”状态时可以执行拒单操作
        if(orders == null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 商家拒单时，如果用户已经完成了支付，需要为用户退款
        Integer payStatus = orders.getPayStatus();
        if(payStatus == Orders.PAID){
            // 需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            log.info("申请退款:{}");
        }

        // 更新订单状态
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        ordersMapper.updateById(orders);
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Long id = ordersCancelDTO.getId();
        Orders orders = ordersMapper.selectById(id);
        // 支付状态
        Integer payStatus = orders.getPayStatus();
        if(payStatus == Orders.PAID){
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            log.info("申请退款:{}");
        }

        // 管理端取消订单，更新订单状态
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        ordersMapper.updateById(orders);
    }

    /**
     * 商家派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders orders = ordersMapper.selectById(id);
        // 校验订单状态,只有状态为“待派送”的订单可以执行派送订单操作
        if(orders == null || !orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        ordersMapper.updateById(orders);
    }

    /**
     * 商家完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders orders = ordersMapper.selectById(id);
        // 校验订单状态，只有状态为“派送中”的订单可以执行订单完成操作
        if(orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 修改订单状态
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        ordersMapper.updateById(orders);
    }
}
