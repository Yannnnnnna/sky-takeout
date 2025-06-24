package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

/**
 * @author wyr on 2025/6/19
 */
public interface OrderService {
    /**
     * 提交订单
     * @param ordersSubmitDTO 订单提交数据传输对象
     * @return 订单提交结果视图对象
     */
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) ;
    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);
    /**
     * 订单历史记录
     * @param page 当前页
     * @param pageSize 每页大小
     * @param status 订单状态
     * @return 分页结果
     */
    PageResult historyOrders(int page, int pageSize, Integer status);

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    OrderVO orderDetail(Long id);

    /**
     * 取消订单
     * @param id
     */
    void cancel(Long id);
    /**
     * 再来一单
     * @param id 订单ID
     */
    void repetition(Long id);

    /**
     * 管理员取消订单
     * @param ordersCancelDTO
     */
    void adminCancel(OrdersCancelDTO ordersCancelDTO);
    /**
     * 管理员订单条件分页查询
     * @param ordersPageQueryDTO 订单分页查询数据传输对象
     * @return 分页结果
     */
    PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 订单统计
     * @return
     */
    OrderStatisticsVO statistics();
    /**
     * 接单
     * @param id 订单ID
     */
    void confirm(Long id);

    /**
     * 拒单
     * @param request
     */
    void reject(OrdersRejectionDTO request);

    /**
     * 配送订单
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成订单
     * @param id
     */
    void complete(Long id);

    /**
     * 催单
     * @param id
     */
    void reminder(Long id);
}
