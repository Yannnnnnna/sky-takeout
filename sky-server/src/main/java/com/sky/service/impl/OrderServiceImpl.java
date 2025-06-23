package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wyr on 2025/6/19
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;

    /**
     * 提交订单
     *
     * @param ordersSubmitDTO 订单提交数据传输对象
     * @return 订单提交结果视图对象
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //校验数据异常
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //抛出异常，地址不存在
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.size() == 0) {
            //抛出异常，购物车数据不存在
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //像订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setAddress(addressBook.getDetail());
        orderMapper.insert(orders);
        //像订单明细表插入n条数据
        List<OrderDetail> orderDetailLists = new ArrayList<>();
        for (ShoppingCart shoppingCartItem : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCartItem, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailLists.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailLists);
        //清空购物车数据
        shoppingCartMapper.delete(userId);
        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        /*JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }*/

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        //为替代微信支付成功后的数据库订单状态更新，多定义一个方法进行修改
        Integer OrderPaidStatus = Orders.PAID; //支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单

        //发现没有将支付时间 check_out属性赋值，所以在这里更新
        LocalDateTime check_out_time = LocalDateTime.now();

        //获取订单号码
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        log.info("调用updateStatus，用于替换微信支付更新数据库状态的问题");
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, orderNumber);

        return vo;
    }


    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 订单历史记录
     *
     * @param page     当前页
     * @param pageSize 每页大小
     * @param status   订单状态
     * @return 分页结果
     */
    @Override
    public PageResult historyOrders(int page, int pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);
        //设置分页查询条件
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        //分页查询
        Page<Orders> pageResult = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        //将查询结果转换为分页结果
        if (pageResult != null && pageResult.getTotal() > 0) {
            for (Orders orders : pageResult) {
                //根据查询结果获取订单详细
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
                if (orderDetails != null && !orderDetails.isEmpty()) {
                    orderVO.setOrderDetailList(orderDetails);
                }
                list.add(orderVO);
            }
        }

        //返回结果
        return new PageResult(pageResult.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO orderDetail(Long id) {
        //根据订单id查询订单
        Orders orders = orderMapper.getById(id);
        //根据订单id查询订单详细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        if (orders == null || orderDetailList == null || orderDetailList.isEmpty()) {
            //抛出异常，订单不存在
            throw new OrderBusinessException("订单不存在");
        }
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 取消订单
     *
     * @param id
     */
    @Override
    public void cancel(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            //抛出异常，订单不存在
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //已接单和派送中订单不允许取消，要电话沟通
        if (orders.getStatus() > 2) {
            throw new OrderBusinessException("订单不可取消，需要联系商家");

        }
//        // 订单处于待接单状态下取消，需要进行退款
//        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
//            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额
//
//            //支付状态修改为 退款
//            orders.setPayStatus(Orders.REFUND);
//        }
        //待支付和待接单订单直接取消
        if (orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            orders.setPayStatus(Orders.REFUND); //退款状态
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);


    }

    /**
     * 再来一单 (工厂流水线版)
     */
    public void repetition(Long id) {
        // 1. 准备原材料
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();
        // 根据订单id，获取所有的“订单详情”原材料
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 2. 流水线开始加工
        // 将“订单详情”原材料列表，送上传送带 (stream())
        // 经过加工站 (map())，将每一个“订单详情”变成一个新的“购物车商品”
        // 最后将所有加工好的“购物车商品”打包成一个列表 (collect())
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            // 这是加工站的具体操作：
            // a. 拿出一个新的空购物车盒子
            ShoppingCart shoppingCart = new ShoppingCart();

            // b. 把旧订单详情(x)的属性（如菜品名称、数量、口味等）复制到新盒子里
            //    忽略掉旧的id，因为新商品要有新id
            BeanUtils.copyProperties(x, shoppingCart, "id");

            // c. 给新盒子贴上当前用户的标签
            shoppingCart.setUserId(userId);

            // d. 盖上当前时间的戳
            shoppingCart.setCreateTime(LocalDateTime.now());

            // e. 将这个加工好的盒子送上传送带的下一段
            return shoppingCart;
        }).collect(Collectors.toList());

        // 3. 一次性入库
        // 将打包好的一整箱“购物车商品”一次性存入数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }
    /**
     * 管理员取消订单
     *
     * @param ordersCancelDTO
     */
    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        if( orders == null || !orders.getStatus().equals(Orders.CONFIRMED) ) {
            //抛出异常
            throw new OrderBusinessException("当前状态不可取消");
        }
        //进行退款和修改状态
        Integer payStatus = orders.getPayStatus();
        if (payStatus.equals(Orders.PAID)) {
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
            //如果已支付，将支付状态更新为“退款”
            orders.setPayStatus(Orders.REFUND);

        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now()); //记录取消时间
        //执行更新
        orderMapper.update(orders);
    }

    /**
     * 管理员订单条件分页查询
     *
     * @param ordersPageQueryDTO 订单分页查询数据传输对象
     * @return 分页结果
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        //设置分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> ordersList = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOList = new ArrayList<>();
        if (ordersList != null || ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
                if (orderDetails != null && !orderDetails.isEmpty()) {
                    orderVO.setOrderDetailList(orderDetails);
                }
                orderVOList.add(orderVO);
            }
        }
        return new PageResult(ordersList.getTotal(), ordersList);

    }

    /**
     * 订单统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        //获取待派件数量
        orderStatisticsVO.setConfirmed(orderMapper.getCountByStatus(Orders.CONFIRMED)); //待接单数量
        //获取派件中数量
        orderStatisticsVO.setDeliveryInProgress(orderMapper.getCountByStatus(Orders.DELIVERY_IN_PROGRESS));
        //获取待接单数量
        orderStatisticsVO.setToBeConfirmed(orderMapper.getCountByStatus(Orders.TO_BE_CONFIRMED));
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param id
     */
    @Override
    public void confirm(Long id) {
        //根据订单id查询订单
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param request 业务规则：
     *
     *   -   商家拒单其实就是将订单状态修改为“已取消”
     *   -   只有订单处于“待接单”状态时可以执行拒单操作
     *   -   商家拒单时需要指定拒单原因
     *   -   商家拒单时，如果用户已经完成了支付，需要为用户退款
     */
    @Override
    public void reject(OrdersRejectionDTO request) {
        // 1. 查询订单并进行严格校验
        Orders ordersDB = orderMapper.getById(request.getId());
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException("订单状态错误，无法拒单");
        }

        // 2. 创建一个用于更新的独立对象，这是最佳实践
        Orders orderToUpdate = new Orders();
        orderToUpdate.setId(ordersDB.getId());

        // 3. 根据支付状态，决定是否需要将支付状态更新为“退款”
        if (ordersDB.getPayStatus().equals(Orders.PAID)) {
            // 如果已支付，将支付状态更新为“退款”
            orderToUpdate.setPayStatus(Orders.REFUND);
            // 此处可以继续保留调用微信退款的逻辑
        }
        // 注意：如果未支付，我们不需要对支付状态做任何操作，因为它本身就是 UN_PAID。

        // 4. 设置订单的最终状态和信息
        orderToUpdate.setStatus(Orders.CANCELLED); // 订单状态更新为“已取消”
        orderToUpdate.setRejectionReason(request.getRejectionReason()); // 设置拒单原因
        orderToUpdate.setCancelTime(LocalDateTime.now()); // 【重要】记录取消时间

        // 5. 执行更新
        orderMapper.update(orderToUpdate);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders == null || !orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException("当前状态不可派送");
        }
        //更新订单状态为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orders.setDeliveryTime(LocalDateTime.now()); //记录派送时间
        orderMapper.update(orders);
    }
    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException("当前状态不可完成");
        }
        //更新订单状态为派送中
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }
}
