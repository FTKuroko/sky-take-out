package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;

/**
 * @author Kuroko
 * @description 菜品管理接口
 * @date 2023/7/13 15:50
 */
public interface DishService extends IService<Dish> {

    /**
     * 根据分类 id 查询菜品数量
     * @param categoryId
     * @return
     */
    Integer countByCategoryId(Long categoryId);

    /**
     * 新增菜品
     * @param dishDTO
     */
    void saveWithFlavor(DishDTO dishDTO);
}
