package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/25 16:23
 */
public interface UserService extends IService<User> {
    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
