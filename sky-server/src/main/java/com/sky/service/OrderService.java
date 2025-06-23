package com.sky.service;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
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
}
