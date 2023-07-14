package com.sky.service;

/**
 * @author Kuroko
 * @description 菜品管理接口
 * @date 2023/7/13 15:50
 */
public interface DishService {

    /**
     * 根据分类 id 查询菜品数量
     * @param categoryId
     * @return
     */
    Integer countByCategoryId(Long categoryId);
}
