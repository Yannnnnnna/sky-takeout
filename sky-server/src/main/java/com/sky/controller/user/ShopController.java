package com.sky.controller.user;

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
@RestController(value = "userShopController")
@RequestMapping("/user/shop")
@Slf4j
@Api(tags ="店铺相关接口")
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

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
