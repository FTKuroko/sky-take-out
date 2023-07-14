package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Kuroko
 * @description 菜品管理 mapper
 * @date 2023/7/13 15:46
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {
}
