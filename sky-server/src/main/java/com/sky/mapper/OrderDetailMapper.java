package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author wyr on 2025/6/19
 */
@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单明细数据
     * @param orderDetailLists
     */
    public void insertBatch(List<OrderDetail> orderDetailLists);
    /**
     * 根据订单ID查询订单明细
     * @param id 订单ID
     * @return 订单明细列表
     */
    @Select("SELECT * FROM order_detail WHERE order_id = #{id}")
    List<OrderDetail> getByOrderId(Long id);
}
