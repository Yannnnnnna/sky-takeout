package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wyr on 2025/6/16
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties wx;
    @Autowired
    private UserMapper userMapper;
    /**
     *
     * 微信用户登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxUserLogin(UserLoginDTO userLoginDTO) {
        log.info("使用微信配置: appid={}, secret={}", wx.getAppid(), wx.getSecret());
        // 调用微信接口服务，获取当前微信用户的openid
        Map<String, String> map = new HashMap<>();
        map.put("appid", wx.getAppid());
        map.put("secret", wx.getSecret());
        map.put("js_code", userLoginDTO.getCode());
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        // 解析json字符串，获取openid
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");


        // 判断openid是否为空，如果为空表示登录失败，抛出业务异常
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        // 判断是否是新用户
        User user = userMapper.selectByOpenid(openid);
        if (user != null) {
            // 如果用户存在，则返回用户信息
            return user;
        }
        // 如果用户不存在，则创建新用户
        user = User.builder()
                .openid(openid)
                .createTime(LocalDateTime.now())
                .build();
        // 保存用户信息到数据库
        userMapper.insert(user);
        // 返回用户信息
        return user;
    }
}
