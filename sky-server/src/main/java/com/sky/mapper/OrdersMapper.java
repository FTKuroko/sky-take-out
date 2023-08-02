package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/2 14:13
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
}
