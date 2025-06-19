package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author wyr on 2025/6/16
 */
@Mapper
public interface UserMapper {
    @Select("select * from user where openid = #{openid}")
     User selectByOpenid(String openid);

    void insert(User user);
    /**
     * 根据用户ID查询用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);
}
