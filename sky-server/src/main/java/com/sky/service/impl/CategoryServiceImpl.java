package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/13 15:52
 */
@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;

    /**
     * 新增分类
     * @param categoryDTO
     */
    @Override
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        // 补全其他字段
        category.setStatus(0);
//        category.setCreateTime(LocalDateTime.now());
//        category.setCreateUser(BaseContext.getCurrentId());
//        category.setUpdateTime(LocalDateTime.now());
//        category.setUpdateUser(BaseContext.getCurrentId());

        categoryMapper.insert(category);
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @Override
    public PageResult<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        // 1. 构造条件构造器
        LambdaQueryWrapper<Category> lqw = new LambdaQueryWrapper<>();
        // 2. 添加查询条件
        if(categoryPageQueryDTO.getType() != null){
            lqw.eq(StringUtils.isNotEmpty(String.valueOf(categoryPageQueryDTO.getType())), Category::getType, categoryPageQueryDTO.getType());
        }
        lqw.like(StringUtils.isNotEmpty(categoryPageQueryDTO.getName()), Category::getName, categoryPageQueryDTO.getName());
        lqw.orderByAsc(Category::getSort);
        // 3. 构造分页对象
        Page<Category> page = new Page<>(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());
        // 4. 查询结果集
        Page<Category> categoryPage = categoryMapper.selectPage(page, lqw);
        // 5. 获取数据列表和数据条数
        List<Category> records = categoryPage.getRecords();
        long total = categoryPage.getTotal();;

        return new PageResult<>(total, records);
    }

    /**
     * 根据 id 删除分类
     * @param id
     */
    @Override
    public void deleteById(Long id) {
        Integer countDish = dishService.countByCategoryId(id);
        if(countDish > 0){
            // 当前分类下关联了菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }
        Integer countSetmeal = setmealService.countByCategoryId(id);
        if(countSetmeal > 0){
            // 当前分类关联了套餐，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        // 什么都没有关联，可以直接删除
        categoryMapper.deleteById(id);
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    @Override
    public void update(CategoryDTO categoryDTO) {
        // TODO: 有个问题，id 是用 Long 来处理的，前端传递过来的 Long 型数据会丢失最后两位精度，导致查不到数据库信息，需要修改
        // 1. 先根据 id 查询对应的分类信息
        Long id = categoryDTO.getId();
        Category category = categoryMapper.selectById(id);
        if(category == null){
            throw new BaseException("未找到相关分类");
        }
        // 2. 对象拷贝
        BeanUtils.copyProperties(categoryDTO, category);
//        // 3. 更新时间和修改人字段
//        category.setUpdateUser(BaseContext.getCurrentId());
//        category.setUpdateTime(LocalDateTime.now());
        // 4. 更新数据库
        categoryMapper.updateById(category);
    }

    /**
     * 启用禁用分类
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Category category = categoryMapper.selectById(id);
        category.setStatus(status);
        categoryMapper.updateById(category);
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @Override
    public List<Category> list(Integer type) {
        return categoryMapper.list(type);
    }
}
