package com.sky.service;

/**
 * @author Kuroko
 * @description 套餐管理接口
 * @date 2023/7/13 15:50
 */
public interface SetmealService {

    /**
     * 根据分类 id 查询套餐数量
     * @param categoryId
     * @return
     */
    Integer countByCategoryId(Long categoryId);
}
