package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author Kuroko
 * @description 分类管理 mapper
 * @date 2023/7/13 15:48
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    List<Category> list(Integer type);
}
