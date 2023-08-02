package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.OrdersMapper;
import com.sky.service.*;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    }


}
