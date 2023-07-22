package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.SetmealDish;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/22 13:29
 */
public interface SetmealDishService extends IService<SetmealDish> {
    /**
     * 根据菜品 id 查询相关套餐
     * @param dishId
     * @return
     */
    List<SetmealDish> selectByDishId(Long dishId);
}
