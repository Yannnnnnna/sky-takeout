package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;

/**
 * @author wyr on 2025/6/16
 */
public interface UserService {
    /**
     * 微信用户登录
     * @param userLoginDTO
     * @return
     */
    User wxUserLogin(UserLoginDTO userLoginDTO);
}
