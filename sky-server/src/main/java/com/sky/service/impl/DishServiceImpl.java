package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import com.sky.service.SetmealDishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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

        // 新增菜品后会导致缓存中的菜品数据与数据库中的菜品数据不一致，因此考虑在新增菜品时先将缓存清空，之后查询数据库时再写入缓存
        Long categoryId = dish.getCategoryId();
        // 清理缓存数据
        String key = "dish_" + categoryId;
        cleanCache(key);
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

    /**
     * 菜品信息分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 构造分页对象
        Page<DishVO> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // 设计多表联查，需要自行编写 SQL
        dishMapper.pageQuery(page, dishPageQueryDTO);
        // 获取数据列表
        List<DishVO> records = page.getRecords();
        // 获取数据总条数
        long total = page.getTotal();
        return new PageResult(total, records);
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        /**
         * 可以一次删除一个菜品，也可以批量删除菜品
         * 起售中的菜品不能删除
         * 被套餐关联的菜品不能删除
         * 删除菜品后，关联的口味数据也需要删除掉
         */
        for(Long id : ids){
            Dish dish = dishMapper.selectById(id);
            // 判断是否还在售卖
            if(dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }

            // 判断是否关联了套餐
            List<SetmealDish> setmealDishes = setmealDishService.selectByDishId(id);
            if(setmealDishes != null && setmealDishes.size() > 0){
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }

            // 删除菜品以及对应的口味表中的数据
            dishMapper.deleteById(id);
            dishFlavorService.deleteByDishId(id);
        }

        // 删除菜品也会导致缓存中的数据和数据库不一致，因此考虑先删除缓存。
        // 只有用户端查询的菜品数据写入缓存，因为管理端访问不会太频繁，并且数据是按照分类存入缓存的。管理端在删除菜品时，可能会删除多个分类下的菜品，简单起见用户端缓存中直接把所有菜品数据清空
        cleanCache("dish_*");
    }

    /**
     * 根据 id 查询菜品信息及口味数据
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.selectById(id);
        DishVO dishVO = new DishVO();
        // 根据菜品 id 查询口味数据
        List<DishFlavor> flavorList = dishFlavorService.getByDishId(id);
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavorList);
        return dishVO;
    }

    /**
     * 修改菜品信息及对应口味信息
     * @param dishDTO
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 修改菜品基本信息
        dishMapper.updateById(dish);
        // 删除原有口味信息
        dishFlavorService.deleteByDishId(dish.getId());
        // 更新口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorService.insertBatch(flavors);
        }

        // 修改菜品信息也会导致缓存与数据库不一致，考虑先删除缓存
        Long categoryId = dish.getCategoryId();
        cleanCache("dish_" + categoryId);
    }

    /**
     * 根据分类 id 查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> listByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(String.valueOf(categoryId)), Dish::getCategoryId, categoryId);
        lqw.eq(Dish::getStatus, StatusConstant.ENABLE);
        lqw.orderByDesc(Dish::getCreateTime);
        List<Dish> dishes = dishMapper.selectList(lqw);
        return dishes;
    }

    /**
     * 起售停售菜品
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 与正在起售的套餐相关联的菜品不能停售
        if(status == StatusConstant.DISABLE){
            List<SetmealDish> setmealDishes = setmealDishService.selectByDishId(id);
            if(setmealDishes != null && setmealDishes.size() > 0){
                setmealDishes.forEach(setmealDish -> {
                    // 套餐 id
                    Long setmealId = setmealDish.getSetmealId();
                    // 获取套餐信息
                    Setmeal setmeal = setmealMapper.selectById(setmealId);
                    // 判断套餐状态
                    if(setmeal.getStatus() == StatusConstant.ENABLE){
                        throw new BaseException("有关联套餐正在售卖，不能停售！");
                    }
                });
            }
        }
        // 修改状态
        Dish dish = dishMapper.selectById(id);
        dish.setStatus(status);
        dishMapper.updateById(dish);

        // 起售停售也会导致缓存与数据库不一致，考虑先删除缓存
        Long categoryId = dish.getCategoryId();
        cleanCache("dish_" + categoryId);
    }

    /**
     * 根据分类 id 动态查询菜品和口味
     * @param dish
     * @return
     */
    @Override
    @Transactional
    public List<DishVO> listWithFlavor(Dish dish) {
        Long categoryId = dish.getCategoryId();
        Integer status = dish.getStatus();
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.isNotEmpty(String.valueOf(categoryId)), Dish::getCategoryId, categoryId);
        lqw.eq(StringUtils.isNotEmpty(String.valueOf(status)), Dish::getStatus, status);
        List<Dish> dishes = dishMapper.selectList(lqw);
        List<DishVO> dishVOList = new ArrayList<>();
        if(dishes != null && dishes.size() > 0){
            dishes.forEach(dish1 -> {
                DishVO dishVO = new DishVO();
                // 查询菜品对应口味
                List<DishFlavor> flavors = dishFlavorService.getByDishId(dish1.getId());
                BeanUtils.copyProperties(dish1, dishVO);
                dishVO.setFlavors(flavors);
                dishVOList.add(dishVO);
            });
        }
        return dishVOList;
    }

    /**
     * 动态查询菜品信息，含有 redis 缓存
     * @param dish
     * @return
     */
    @Override
    public List<DishVO> listCache(Dish dish) {
        // 创建 key
        Long categoryId = dish.getCategoryId();
        String key = "dish_" + categoryId;
        // 查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 缓存命中
        if(json != null){
            // json 转列表对象
            try {
                List<DishVO> dishVOList = objectMapper.readValue(json, new TypeReference<List<DishVO>>() {
                });
                return dishVOList;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        // 缓存未命中，查询数据库
        List<DishVO> dishVOList = listWithFlavor(dish);
        // 列表对象转 json，存入 redis
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(dishVOList));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return dishVOList;
    }

    /**
     * 清除缓存菜品数据
     * @param pattern
     */
    private void cleanCache(String pattern){
        Set<String> keys = stringRedisTemplate.keys(pattern);
        stringRedisTemplate.delete(keys);
    }
}
