package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * @author Kuroko
 * @description 菜品管理 mapper
 * @date 2023/7/13 15:46
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 菜品信息分页查询
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(@Param("page") Page<DishVO> page, @Param("query") DishPageQueryDTO dishPageQueryDTO);
}
