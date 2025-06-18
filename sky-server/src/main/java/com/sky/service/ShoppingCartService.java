package com.sky.service;

import com.sky.dto.ShoppingCartDTO;

/**
 * @author wyr on 2025/6/18
 */
public interface ShoppingCartService {
    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void add(ShoppingCartDTO shoppingCartDTO);
}
