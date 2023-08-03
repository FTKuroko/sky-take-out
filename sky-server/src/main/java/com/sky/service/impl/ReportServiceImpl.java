package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.OrdersService;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/3 17:29
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 根据时间区间统计营业额
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnover(LocalDate begin, LocalDate end) {
        // 记录从 begin 到 end 范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        // 从 begin 开始，每加一天都加入集合中
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        // 将集合拼接成字符串，每个元素之间用 "," 隔开
        String dataListStr = StringUtils.join(dateList, ",");

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            Double aDouble = ordersMapper.sumAmountInOneDay(Orders.COMPLETED, date);
            aDouble = aDouble == null ? 0.0 : aDouble;
            turnoverList.add(aDouble);
        }
        String turnoverStr = StringUtils.join(turnoverList, ",");

        return TurnoverReportVO
                .builder()
                .dateList(dataListStr)
                .turnoverList(turnoverStr)
                .build();
    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> newUserList = new ArrayList<>();  // 新增用户数
        List<Integer> totalUserList = new ArrayList<>();    // 总用户数
        for (LocalDate date : dateList) {
            LambdaQueryWrapper<User> lqw1 = new LambdaQueryWrapper<>();
            LambdaQueryWrapper<User> lqw2 = new LambdaQueryWrapper<>();
            lqw1.like(User::getCreateTime, date);
            Integer newUser = userMapper.selectCount(lqw1);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            lqw2.lt(User::getCreateTime, endTime);
            Integer totalUser = userMapper.selectCount(lqw2);

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 根据时间区间统计订单数量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        // 每天订单总数
        List<Integer> orderCountList = new ArrayList<>();
        // 每天有效订单数(完成订单)
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LambdaQueryWrapper<Orders> lqw1 = new LambdaQueryWrapper<>();
            LambdaQueryWrapper<Orders> lqw2 = new LambdaQueryWrapper<>();
            lqw1.like(Orders::getOrderTime, date);
            lqw2.eq(Orders::getStatus, Orders.COMPLETED).like(Orders::getOrderTime, date);
            Integer orderCount = ordersMapper.selectCount(lqw1);
            Integer completeOrderCount = ordersMapper.selectCount(lqw2);
            orderCountList.add(orderCount);
            validOrderCountList.add(completeOrderCount);
        }
        // 区间内订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        // 区间内有效订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        // 订单完成率
        Double orderCompleteRate = 0.0;
        if(totalOrderCount != 0){
            orderCompleteRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompleteRate)
                .build();
    }

    /**
     * 查询指定时间区间内的销量排名top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end){
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOList = ordersMapper.getSalesTop10(beginTime, endTime);

        String nameList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()),",");
        String numberList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()),",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}
