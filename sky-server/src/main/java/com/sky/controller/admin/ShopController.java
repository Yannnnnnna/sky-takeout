package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @author wyr on 2025/6/13
 */
@RestController(value = "adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@Api(tags ="店铺相关接口")
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;
    @PutMapping("/{status}")
    @ApiOperation(value = "设置营业状态")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置营业状态:{}",status==1?"营业中":"打烊中");
        // 将状态存入Redis
        redisTemplate.opsForValue().set("SHOP_STATUS", status);
        return Result.success("设置成功");
    }
    @ApiOperation(value = "获取店铺营业状态")
    @GetMapping("/status")
    public Result getStatus() {
        // 从Redis中获取状态
        Integer status = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
        if (status == null) {
            return Result.error("店铺状态未设置");
        }
        log.info("获取营业状态:{}",status==1?"营业中":"打烊中");
        return Result.success(status);
    }
}
