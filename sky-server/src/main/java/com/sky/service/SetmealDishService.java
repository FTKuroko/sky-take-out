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

    /**
     * 批量插入套餐及菜品关系
     * @param setmealDishes
     * @return
     */
    boolean insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐 id 删除对应菜品关系
     * @param setmealId
     */
    void deleteBySetmealId(Long setmealId);

    /**
     * 根据套餐 id 查询套餐菜品关系
     * @param setmealId
     * @return
     */
    List<SetmealDish> selectBySetmealId(Long setmealId);
}
