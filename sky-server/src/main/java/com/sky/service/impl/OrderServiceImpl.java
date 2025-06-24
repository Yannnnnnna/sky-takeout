package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.webSocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wyr on 2025/6/19
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

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
    @Autowired
    private WebSocketServer webSocketServer;

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
        //检查用户的收货地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName()+ addressBook.getDistrictName() + addressBook.getDetail());
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
        //通过WebSocket向客户端推送消息 type,orderId, content
        Map map = new HashMap();
        map.put("type", 1);
//        map.put("orderId", ordersPaymentDTO.);
        map.put("content", "订单号：" + orderNumber);
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
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

    /**
     * 检查客户的收货地址是否超出配送范围。
     * <p>
     * 此方法通过调用百度地图服务来计算从店铺到客户地址的实际驾驶距离。
     * 如果距离超过设定的阈值（在此代码中为100公里），则认为超出配送范围并抛出异常。
     *
     * @param address 客户提供的收货地址字符串。
     * @throws OrderBusinessException 当地址解析失败、路线规划失败或计算出的距离超出范围时抛出。
     */
    private void checkOutOfRange(String address) {
        // 1. 准备API请求参数
        // 创建一个Map来存储发送给百度地图API的参数。
        // 注意: 这个Map在后续的多个API调用中被复用，这可能引入风险，
        // 因为旧的参数可能会被带入到不相关的API调用中。
        Map map = new HashMap<>();
        map.put("address", shopAddress); // 设置要查询的地址为店铺地址
        map.put("output", "json");       // 指定返回格式为JSON
        map.put("ak", ak);               // 提供开发者密钥

        // 2. 获取店铺的经纬度坐标
        // 调用百度地图的“地理编码服务”，将文字地址转换为地理坐标。
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        // 将返回的JSON字符串解析成一个JSONObject对象以便操作。
        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        // 检查API调用是否成功。百度地图API成功时status为"0"。
        if (!"0".equals(jsonObject.getString("status"))) {
            // 如果失败，则无法继续，抛出异常。
            throw new OrderBusinessException("店铺地址解析失败");
        }

        // 从返回的JSON中提取经纬度信息。
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat"); // 获取纬度
        String lng = location.getString("lng"); // 获取经度
        // 将纬度和经度拼接成百度地图API要求的格式 "纬度,经度"。
        String shopLngLat = lat + "," + lng;

        // 3. 获取用户收货地址的经纬度坐标
        // 复用同一个map，仅更新address参数为用户的收货地址。
        map.put("address", address);
        // 再次调用“地理编码服务”来解析用户地址。
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        // 同样，检查API调用是否成功。
        if (!"0".equals(jsonObject.getString("status"))) {
            throw new OrderBusinessException("收货地址解析失败");
        }

        // 提取用户的经纬度。
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        // 拼接成用户地址的坐标字符串。
        String userLngLat = lat + "," + lng;

        // 4. 规划配送路线并计算距离
        // 更新map，为“路线规划服务”准备参数。
        map.put("origin", shopLngLat);          // 设置路线的起点（店铺坐标）
        map.put("destination", userLngLat);     // 设置路线的终点（用户坐标）
        map.put("steps_info", "0");             // "0"表示不需要返回详细的导航步骤，可以优化响应速度

        // 调用百度地图的“轻量级路线规划服务（驾车）”，计算两点间的驾驶路线。
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        // 检查路线规划API调用是否成功。
        if (!"0".equals(jsonObject.getString("status"))) {
            throw new OrderBusinessException("配送路线规划失败");
        }

        // 从返回结果中解析出路线距离。
        JSONObject result = jsonObject.getJSONObject("result");
        // "routes"是一个数组，可能包含多条备选路线，这里我们取第一条推荐路线。
        JSONArray jsonArray = (JSONArray) result.get("routes");
        // 从路线信息中获取"distance"字段，其单位为米。
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        // 5. 判断距离是否超出配送范围
        // 检查计算出的距离是否大于100000米（即100公里）。
        if (distance > 100000) {
            // 如果超过了设定的最大配送距离，就抛出异常。
            // 注意：原注释为“配送距离超过5000米”，但代码中的数字是100000。
            throw new OrderBusinessException("超出配送范围");
        }

        // 如果程序能执行到这里，说明所有检查都已通过，地址在配送范围内。
    }
}
