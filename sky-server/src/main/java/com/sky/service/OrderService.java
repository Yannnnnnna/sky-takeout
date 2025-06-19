package com.sky.service;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;

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

}
