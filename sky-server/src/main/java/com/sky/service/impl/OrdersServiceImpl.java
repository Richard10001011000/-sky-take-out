package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrdersService;
import com.sky.utils.BeanUtil;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 12548
* @description 针对表【orders(订单表)】的数据库操作Service实现
* @createDate 2024-08-28 12:48:59
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders>
    implements OrdersService{

    private final OrdersMapper orderMapper;

    private final OrderDetailMapper orderDetailMapper;

    private final ShoppingCartMapper shoppingCartMapper;

    private final AddressBookMapper addressBookMapper;

    private final UserMapper userMapper;

    private final WeChatPayUtil weChatPayUtil;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

//    private WebSocketServer webSocketServer;

    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //        异常情况的处理（收货地址为空、超出配送氛围、购物车为空）
        AddressBook addressBook = addressBookMapper.selectById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //        检查用户的收货地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.selectList(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, BaseContext.getCurrentId()));
        if(CollectionUtils.isEmpty(shoppingCarts)){
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(BaseContext.getCurrentId());
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());
        orderMapper.insert(order);
        //        订单明细数据
        List<OrderDetail> orderDetailList = BeanUtil.copyList(shoppingCarts,OrderDetail.class);
        orderDetailList.forEach(orderDetail -> orderDetail.setOrderId(order.getId()));
        orderDetailMapper.insert(orderDetailList);

        shoppingCartMapper.delete(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, BaseContext.getCurrentId()));

        //        封装返回结果
        OrderSubmitVO submitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

        return submitVO;

    }

    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        User user = userMapper.selectById(BaseContext.getCurrentId());

        String orderNumber = ordersPaymentDTO.getOrderNumber();
        Orders orders = orderMapper.selectOne(new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, user.getId()).eq(Orders::getNumber,orderNumber));

        JSONObject jsonObject = weChatPayUtil.pay(orderNumber,orders.getAmount(),"苍穹外卖订单" + orders.getId(), user.getOpenid());
        if(jsonObject.getString("code")!=null && jsonObject.getString("code").equals("ORDERPAID")){
            throw new OrderBusinessException("该订单已支付");
        }
        OrderPaymentVO orderPaymentVO = jsonObject.toJavaObject(OrderPaymentVO.class);
        orderPaymentVO.setPackageStr(jsonObject.getString("package"));
        return orderPaymentVO;
    }

    @Override
    public PageResult pageQueryForUser(int pageNum, int pageSize, Integer status) {

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setPage(pageNum);
        ordersPageQueryDTO.setPageSize(pageSize);
//        分页条件查询
        IPage<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<Orders> ordersList = page.getRecords();
        ArrayList<OrderVO> list = new ArrayList<>();

//        查询出订单明细，并封装入OrderVo进行响应
        if (!CollectionUtils.isEmpty(ordersList)) {
            ordersList.forEach(orders -> {
                Long ordersId = orders.getId();

//                查询订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.selectList(
                        new LambdaQueryWrapper<OrderDetail>()
                                .eq(OrderDetail::getOrderId, ordersId));

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            });
        }

        return new PageResult(page.getTotal(), list);
    }

    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.selectById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtil.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    @Override
    public void userCancelById(Long id) throws Exception {
        //        根据id查询订单
        Orders orderDB = orderMapper.selectById(id);

//        校验订单是否存在
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

//      订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (orderDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(orderDB.getId());

//        订单处于待接单的状态下取消，需要进行退款
        if (orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
//            调用微信支付退款接口
            weChatPayUtil.refund(
                    orderDB.getNumber(),
                    orderDB.getNumber(),
                    orders.getAmount(),
                    orders.getAmount()
            );

//            支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

//        更新订单状态，取消原因、时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(orders);
    }

    @Override
    public void repetition(Long id) {
//        查询当前用户id
        Long userId = BaseContext.getCurrentId();

//        根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

//        将订单详情对象转为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();

//            将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

//        将购物车对象批量添加到购物车
        shoppingCartMapper.insert(shoppingCartList);
    }

    @Override
    public void reminder(Long id) {
////        查询订单是否存在
//        Orders orders = orderMapper.selectById(id);
//        if (orders == null) {
//            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
//        }
//
////        基于WebSocket实现催单
//        HashMap map = new HashMap();
//        map.put("type", 2);
//        map.put("orderId", id);
//        map.put("content", "订单号：" + orders.getNumber());
//        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    @Override
    public void paySuccess(String outTradeNo) {
//        当前登录用户id
        Long userId = BaseContext.getCurrentId();

//        根据订单号查询当前用户的订单
        Orders orderDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

//        根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = orderMapper.selectById(orderDB.getId());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.TO_BE_CONFIRMED);
        orders.setPayStatus(Orders.PAID);
        orders.setCheckoutTime(LocalDateTime.now());
        orderMapper.updateById(orders);

        HashMap map = new HashMap();
        map.put("type", 1);
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + outTradeNo);

//        通过WebSocket实现来电提醒，向客户端浏览器推送消息
//        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        //        根据id查询订单
        Orders orderDB = orderMapper.selectById(ordersCancelDTO.getId());

//        支付状态
        Integer payStatus = orderDB.getPayStatus();
        if (payStatus == 1) {
//            用于已支付，需要退款
            String refund = weChatPayUtil.refund(
                    orderDB.getNumber(),
                    orderDB.getNumber(),
                    orderDB.getAmount(),
                    orderDB.getAmount()
            );
            log.info("申请退款：{}", refund);
        }

//      管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = orderMapper.selectById(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(orders);
    }

    @Override
    public void delivery(Long id) {
//        根据id查询订单
        Orders orderDB = orderMapper.selectById(id);

//        校验订单是否存在，并且状态为3
        if (orderDB == null || !orderDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = orderMapper.selectById(id);
//        更新订单状态，状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.updateById(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        //        根据id查询订单
        Orders ordersDB = orderMapper.selectById(ordersRejectionDTO.getId());

//        订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

//        支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (Orders.PAID.equals(payStatus)) {
//            用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    ordersDB.getAmount(),
                    ordersDB.getAmount()
            );
            log.info("申请退款：{}", refund);
        }

//        拒单需要退款，根据订单id更新订单状态，拒单原因，取消时间
        Orders orders = orderMapper.selectById(ordersRejectionDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.updateById(orders);
    }

    @Override
    public void confirm(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.selectById(ordersCancelDTO.getId());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.updateById(orders);
    }

    @Override
    public OrderStatisticsVO statistics() {
        //        根据状态，分别查询出接待单，待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

//        将查询出的数据封装
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        IPage<Orders> pageQuery = orderMapper.pageQuery(ordersPageQueryDTO);

//        部分订单状态，需要额外返回订单菜品信息，将orders转化为orderVo
        List<OrderVO> orderVoList = getOrderVoList(pageQuery.getRecords());
        return new PageResult(pageQuery.getTotal(), orderVoList);
    }

    @Override
    public void complete(Long id) {
//        根据id查询订单
        Orders orderDB = orderMapper.selectById(id);

//        校验订单是否存在，并且状态为4
        if (orderDB == null || !orderDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = orderMapper.selectById(id);

//        更新订单状态，状态转为完成
        orders.setStatus(Orders.CONFIRMED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.updateById(orders);
    }

    private void checkOutOfRange(String address) {
        HashMap map = new HashMap();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);

//        获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }

//        数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");

//        店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address", address);

//        获取用户地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

//        数据解析
        location = JSON.parseObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");

//        用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("orgin", shopLngLat);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("配送线路规划失败");
        }

//        数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }

    private List<OrderVO> getOrderVoList( List<Orders> ordersList) {
//        需要返回订单菜品信息，自定义OrderVo响应结果
        ArrayList<OrderVO> orderVOArrayList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(ordersList)) {
            ordersList.forEach(orders -> {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);

                String orderDishStr = getOrderDishStr(orders);
                orderVO.setOrderDishes(orderDishStr);
                orderVOArrayList.add(orderVO);
            });
        }
        return orderVOArrayList;
    }

    private String getOrderDishStr(Orders orders) {
//        查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

//        将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> ordewrDishList = orderDetailList.stream().map(orderDetail -> orderDetail.getName() + "*" + orderDetail.getNumber() + ";").collect(Collectors.toList());

//        将该订单对应的所有菜品信息拼接在一起
        return String.join("", ordewrDishList);
    }
}




