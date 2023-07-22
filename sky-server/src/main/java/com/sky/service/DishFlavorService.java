package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.DishFlavor;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/15 11:23
 */
public interface DishFlavorService extends IService<DishFlavor> {
    /**
     * 根据菜品 id 删除相关口味表信息
     * @param dishId
     */
    void deleteByDishId(Long dishId);

    /**
     * 根据菜品 id 查询对应口味数据
     * @param dishId
     * @return
     */
    List<DishFlavor> getByDishId(Long dishId);

    /**
     * 批量插入口味信息
     * @param flavors
     */
    void insertBatch(List<DishFlavor> flavors);
}
