package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author wyr on 2025/5/10
 */
@Mapper
public interface CategoryMapper {

    @Select("select * from cagegory where id = #{id}")
    Category selectById(Integer id);

    Page<Category> pageSelect(CategoryPageQueryDTO categoryPageQueryDTO);
}
