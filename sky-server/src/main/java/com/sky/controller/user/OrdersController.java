package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/2 14:09
 */
@RestController
@RequestMapping("/user/order")
@Slf4j
@Api(tags = "用户订单管理接口")
public class OrdersController {
    @Autowired
    private OrdersService ordersService;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单信息:{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = ordersService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO){
        log.info("订单支付:{}", ordersPaymentDTO);
        OrderPaymentVO paymentVO = ordersService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单:{}", paymentVO);
        return Result.success(paymentVO);
    }
}
