package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.service.SetmealDishService;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
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
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private DishMapper dishMapper;

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

    /**
     * 新增套餐，需要同时保存套餐以及套餐和菜品对应关系
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 向套餐表插入数据
        setmealMapper.insert(setmeal);
        // 获取生成的套餐 id
        Long setmealId = setmeal.getId();
        // 操作套餐和菜品对应关系表
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        // 保存套餐和菜品关系
        setmealDishService.insertBatch(setmealDishes);
    }

    /**
     * 套餐信息分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        // 构造分页对象
        Page<SetmealVO> page = new Page<>(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        // 涉及多表查询，套餐信息中的分类信息需要从分类表中查询
        setmealMapper.pageQuery(page, setmealPageQueryDTO);
        // 获取数据列表和数据总条数
        List<SetmealVO> records = page.getRecords();
        long total = page.getTotal();
        return new PageResult(total, records);
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        /**
         * 可以一次删除一个套餐，也可以批量删除套餐
         * 起售中的套餐不能删除
         * 删除套餐后，套餐菜品关系表中关联的菜品信息也需要删除掉
         */
        for(Long id : ids){
            Setmeal setmeal = setmealMapper.selectById(id);
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                // 起售中的菜品不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
            // 删除套餐
            setmealMapper.deleteById(id);
            // 删除对应的套餐菜品关系
            setmealDishService.deleteBySetmealId(id);
        }
    }

    /**
     * 根据 id 查询套餐，用于修改页面回显数据
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.selectById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        List<SetmealDish> setmealDishes = setmealDishService.selectBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 修改套餐表
        setmealMapper.updateById(setmeal);
        // 删除原有菜品关联信息
        setmealDishService.deleteBySetmealId(setmeal.getId());
        // 更新菜品套餐关联信息
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealDTO.getId());
            });
            setmealDishService.insertBatch(setmealDishes);
        }
    }

    /**
     * 起售停售套餐
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 如果要起售套餐，首先要判断套餐内菜品是否都是起售状态
        if(status == StatusConstant.ENABLE){
            List<SetmealDish> setmealDishes = setmealDishService.selectBySetmealId(id);
            if(setmealDishes != null && setmealDishes.size() > 0){
                setmealDishes.forEach(setmealDish -> {
                    // 获取菜品 id
                    Long dishId = setmealDish.getDishId();
                    // 获取菜品信息
                    Dish dish = dishMapper.selectById(dishId);
                    if(dish.getStatus() == StatusConstant.DISABLE){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        // 修改套餐状态
        Setmeal setmeal = setmealMapper.selectById(id);
        setmeal.setStatus(status);
        setmealMapper.updateById(setmeal);
    }


}
