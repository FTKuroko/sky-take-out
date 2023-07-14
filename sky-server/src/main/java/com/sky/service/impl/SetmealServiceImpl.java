package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Setmeal;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/13 15:51
 */
@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据分类 id 查询套餐数量
     * @param categoryId
     * @return
     */
    @Override
    public Integer countByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(String.valueOf(categoryId)), Setmeal::getCategoryId, categoryId);
        Integer count = setmealMapper.selectCount(lqw);
        return count;
    }
}
