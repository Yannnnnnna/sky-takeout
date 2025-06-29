package com.sky.mapper;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author wyr on 2025/6/18
 */
@Mapper
public interface ShoppingCartMapper {

    /**
     * 动态查询
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);
    /**
     * 更新购物车数量
     * @param cart
     */
    @Update("UPDATE shopping_cart SET number = #{number} WHERE id = #{id}")
    void updateNumberById(ShoppingCart cart);

    /**
     * 插入购物车
     * @param shoppingCart
     */
    @Insert("INSERT INTO shopping_cart (name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time) " +
            "VALUES (#{name}, #{image}, #{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount}, #{createTime})")
    void insert(ShoppingCart shoppingCart);

    /**
     * 清空购物车
     * @param userId
     */
    @Delete("DELETE FROM shopping_cart WHERE user_id = #{userId}")
    void delete(Long userId);
    /**
     * 批量插入购物车数据
     * @param shoppingCartList 购物车列表
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
