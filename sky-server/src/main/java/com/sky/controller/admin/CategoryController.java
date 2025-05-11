package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;

import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.ApiOperation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author wyr on 2025/5/10
 */
@RestController
@RequestMapping("/admin/category")
@Slf4j
public class CategoryController {
    @Autowired
    CategoryService categoryService;

    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改分类")
    public Result updateCategory(@RequestBody CategoryDTO categoryDTO){
        System.out.println(categoryDTO);
        categoryService.updateCategory(categoryDTO);
        return Result.success();
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        PageResult pageResult = categoryService.pageSelect(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 更新分类状态
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("更新状态")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("更新状态");
        System.out.println(id + " 状态 " + status);
        categoryService.updateStatus(status, id);
        return Result.success();
    }

    /**
     * 添加分类
     * @param categoryDTO
     * @return
     */
    @PostMapping
    @ApiOperation("添加分类")
    public Result addCategory(@RequestBody CategoryDTO categoryDTO){
        System.out.println(categoryDTO);
        categoryService.addCategory(categoryDTO);
        return Result.success();
    }
//    @DeleteMapping
//    @ApiOperation("删除分类")
//    public Result deleteCategory(Long id){
//        return Result.success();
//    }

    /**
     * 根据id查询分类
     * @param id
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据id查询分类")
    public Result<List<Category>> selectByID(Integer id){
        log.info("查找分类");
        List<Category> list= categoryService.selectById(id);
        return Result.success(list);
    }

}
