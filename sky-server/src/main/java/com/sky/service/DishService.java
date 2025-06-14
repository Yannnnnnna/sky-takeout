package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

/**
 * @author wyr on 2025/6/10
 */
public interface DishService {
    /**
     * 新增菜品
     *
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO);
    /**
     * 分页查询菜品列表
     *
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 批量删除菜品
     * @param ids
     */
    void delete(List<Long> ids);

    /**
     * 根据菜品id查询菜品信息
     * @param id
     * @return
     */
    DishVO getById(Long id);

    /**
     * 更新菜品信息
     * @param dishDTO
     */
    void update(DishDTO dishDTO);
}
