package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Kuroko
 * @description 套餐管理 mapper
 * @date 2023/7/13 15:48
 */
@Mapper
public interface SetmealMapper extends BaseMapper<Setmeal> {
    /**
     * 套餐信息分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(@Param("page") Page page, @Param("query") SetmealPageQueryDTO setmealPageQueryDTO);
}
