package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishMapper;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/13 15:51
 */
@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 根据分类 id 查询菜品数量
     * @param categoryId
     * @return
     */
    @Override
    public Integer countByCategoryId(Long categoryId) {
        log.info("根据分类 id 查询菜品数量:{}", categoryId);
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(String.valueOf(categoryId)), Dish::getCategoryId, categoryId);
        Integer count = dishMapper.selectCount(lqw);
        return count;
    }

    /**
     * 新增菜品
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO){
        log.info("新增菜品:{}", dishDTO);
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 向菜品表中插入一条数据
        dishMapper.insert(dish);
        // 获取插入的菜品 id
        Long dishId = getByName(dishDTO.getName()).getId();
        // 获取菜品口味集合，向口味表插入 n 条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });

            dishFlavorService.saveBatch(flavors);
        }
    }

    /**
     * 根据菜品名称查询菜品
     * @param name
     * @return
     */
    public Dish getByName(String name){
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(name), Dish::getName, name);
        Dish dish = dishMapper.selectOne(lqw);
        return dish;
    }
}
