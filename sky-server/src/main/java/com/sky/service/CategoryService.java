package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

import java.util.List;

/**
 * @author wyr on 2025/5/10
 */
public interface CategoryService {
    /**
     * 根据id查找种类
     * @param id
     * @return
     */
    List<Category> selectById(Integer id);

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult pageSelect(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 更新状态
     * @param status
     * @param id
     */
    void updateStatus(Integer status, Long id);

    /**
     * 添加分类
     * @param categoryDTO
     */
    void addCategory(CategoryDTO categoryDTO);

    /**
     * 修改分类
     * @param categoryDTO
     */
    void updateCategory(CategoryDTO categoryDTO);

    /**
     * 删除分类
     * @param id
     */
    void delete(Long id);
}
