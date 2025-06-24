package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wyr on 2025/6/24
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    /** * 处理超时订单
     * 1. 查询超时订单
     * 2. 修改订单状态为已取消
     * 3. 发送消息通知用户
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟执行一次
    public void processTimeoutOrder(){
        log.info("开始处理超时订单");
        Integer status = Orders.PENDING_PAYMENT; // 待付款状态
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> timeoutOrders = orderMapper.getTimeoutOrders(status, time);
        if(timeoutOrders != null && !timeoutOrders.isEmpty()) {
            for (Orders orders : timeoutOrders) {
                orders.setStatus(Orders.CANCELLED); // 设置订单状态为已取消
                orders.setCancelReason("支付超时,自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }

    }
    /**
     * 处理一直处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行一次
    public void processDeliveryOrder(){
        log.info("处理一直处于派送中的订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> deliveryOrders = orderMapper.getTimeoutOrders(Orders.DELIVERY_IN_PROGRESS, time);
        if(deliveryOrders != null && !deliveryOrders.isEmpty()) {
            for (Orders orders : deliveryOrders) {
                orders.setStatus(Orders.COMPLETED); // 设置订单状态为已取消
                orderMapper.update(orders);
            }
        }
    }
}
