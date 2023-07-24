package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.service.SetmealDishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/22 13:29
 */
@Service
@Slf4j
public class SetmealDishServiceImpl extends ServiceImpl<SetmealDishMapper, SetmealDish> implements SetmealDishService {
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 根据菜品 id 查询套餐
     * @param dishId
     * @return
     */
    @Override
    public List<SetmealDish> selectByDishId(Long dishId) {
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(String.valueOf(dishId)), SetmealDish::getDishId, dishId);
        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(lqw);
        return setmealDishes;
    }

    /**
     * 批量插入套餐及菜品关系
     * @param setmealDishes
     * @return
     */
    @Override
    public boolean insertBatch(List<SetmealDish> setmealDishes) {
        return this.saveBatch(setmealDishes);
    }

    /**
     * 根据套餐 id 进行删除
     * @param setmealId
     */
    @Override
    public void deleteBySetmealId(Long setmealId) {
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(String.valueOf(setmealId)), SetmealDish::getSetmealId, setmealId);
        setmealDishMapper.delete(lqw);
    }

    @Override
    public List<SetmealDish> selectBySetmealId(Long setmealId) {
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(String.valueOf(setmealId)), SetmealDish::getSetmealId, setmealId);
        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(lqw);
        return setmealDishes;
    }
}
