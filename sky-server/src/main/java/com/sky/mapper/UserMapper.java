package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/25 16:23
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
