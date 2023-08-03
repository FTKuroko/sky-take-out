package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/2 14:13
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

    /**
     * 查询一天的营业额
     * @param status
     * @param date
     * @return
     */
    Double sumAmountInOneDay(Integer status, LocalDate date);

    /**
     * 查询商品销量排名
     * @param begin
     * @param end
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);
}
