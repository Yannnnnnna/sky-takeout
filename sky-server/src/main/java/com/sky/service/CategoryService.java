package com.sky.service;

import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

/**
 * @author wyr on 2025/5/10
 */
public interface CategoryService {
    /**
     * 根据id查找种类
     * @param id
     * @return
     */
    Category selectById(Integer id);

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult pageSelect(CategoryPageQueryDTO categoryPageQueryDTO);
}
