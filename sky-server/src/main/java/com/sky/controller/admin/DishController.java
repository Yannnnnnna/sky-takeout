package com.sky.controller.admin;

import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 *
 * @author wyr on 2025/6/10
 */
@RestController
@Api(tags = "菜品相关接口")
@Slf4j
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品
     *
     * @param dishDTO
     * @return
     */
    @ApiOperation("新增菜品")
    @PostMapping()
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        // 清除缓存
        String key = "dish_" +dishDTO.getCategoryId();
        redisTemplate.delete(key);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();

    }
    /**
     * 分页查询菜品列表
     */
    @ApiOperation("分页查询菜品列表")
     @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    /**
     * 批量删除菜品
     */
    @ApiOperation("删除菜品")
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids) {
        log.info("删除菜品，id：{}", ids);
        // 清除缓存,清除所有菜品分类下的菜品缓存
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        dishService.delete(ids);
        return Result.success();
    }
    /**
     * 根据ID查找商品
     * @param id
     * @return
     */
    @ApiOperation("根据ID查找商品")
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据ID查找商品：{}", id);
        DishVO dishVO = dishService.getById(id);
        if (dishVO == null) {
            return Result.error("菜品不存在");
        }
        return Result.success(dishVO);
    }
    /**
     * 更新菜品信息
     * @param dishDTO
     * @return
     */
    @ApiOperation("更新菜品信息")
    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("更新菜品信息：{}", dishDTO);
        // 清除缓存,清除所有菜品分类下的菜品缓存
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        dishService.update(dishDTO);
        return Result.success();
    }
    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        List<DishVO> list = dishService.listWithFlavor(dish);

        return Result.success(list);
    }
    /**
     * 起售停售商品
     *
     * @param id
     * @return
     */
    @ApiOperation("起售停售商品")
    @PostMapping("/status/{status}")
    public Result status(@PathVariable Integer status, Long id) {
        log.info("起售停售商品，id：{}，status：{}", id, status);
        dishService.startStop(status,id);
        // 清除缓存,清除所有菜品分类下的菜品缓存
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        return Result.success();
    }
}
