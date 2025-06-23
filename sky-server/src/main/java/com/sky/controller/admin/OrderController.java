package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author wyr on 2025/6/23
 */
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Api(tags = "订单管理接口")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 订单条件分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单条件分页")
    public Result<PageResult> shoppingCartList(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("订单条件分页: {}", ordersPageQueryDTO);
        PageResult pageResult = orderService.pageQuery(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 订单统计
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("订单统计")
    public Result<OrderStatisticsVO>  statistics(){
        log.info("订单统计");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }


    /**
     * 订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("订单详情")
    public Result orderDetail(@PathVariable Long id) {
        log.info("订单详情: {}", id);
        return Result.success(orderService.orderDetail(id));
    }

    /**
     * 接单
     * @param orderConfirmDTO
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO orderConfirmDTO) {
        log.info("接单: {}", orderConfirmDTO);
        orderService.confirm(orderConfirmDTO.getId());
        return Result.success();
    }
    /**
     * 拒单
     * @param request
     * @return
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result reject(@RequestBody OrdersRejectionDTO request) {
        log.info("拒单: {}", request);
        orderService.reject(request);
        return Result.success();
    }
    /**
     * 管理员取消订单
     */
    @PutMapping("/cancel")
    @ApiOperation("管理员取消订单")
    public Result adminCancel(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        log.info("管理员取消订单: {}", ordersCancelDTO);
        orderService.adminCancel(ordersCancelDTO);
        return Result.success();
    }
    /** * 订单配送
     * @param id 订单ID
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("订单配送")
    public Result delivery(@PathVariable Long id) {
        log.info("订单配送: {}", id);
        orderService.delivery(id);
        return Result.success();
    }
    /**
     * 完成订单
     * @param id
     * @return
     */
    @PutMapping("complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable Long id) {
        log.info("完成订单: {}", id);
        orderService.complete(id);
        return Result.success();
    }
}
