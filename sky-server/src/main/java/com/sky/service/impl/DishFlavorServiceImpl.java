package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.service.DishFlavorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/15 11:24
 */
@Slf4j
@Service
public class DishFlavorServiceImpl extends ServiceImpl<DishFlavorMapper, DishFlavor> implements DishFlavorService {
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    /**
     * 根据菜品 id 删除口味表中对应信息
     * @param dishId
     */
    @Override
    public void deleteByDishId(Long dishId) {
        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(String.valueOf(dishId)), DishFlavor::getDishId, dishId);
        dishFlavorMapper.delete(lqw);
    }

    /**
     * 根据菜品 id 查询菜品口味数据
     * @param dishId
     * @return
     */
    @Override
    public List<DishFlavor> getByDishId(Long dishId) {
        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(String.valueOf(dishId)), DishFlavor::getDishId, dishId);
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(lqw);
        return dishFlavors;
    }

    /**
     * 批量插入口味信息
     * @param flavors
     */
    @Override
    public void insertBatch(List<DishFlavor> flavors) {
        this.saveBatch(flavors);
    }
}
