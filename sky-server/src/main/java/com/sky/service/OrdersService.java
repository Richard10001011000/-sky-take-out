package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

/**
* @author 12548
* @description 针对表【orders(订单表)】的数据库操作Service
* @createDate 2024-08-28 12:48:59
*/
public interface OrdersService extends IService<Orders> {

    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    PageResult pageQueryForUser(int pageNum, int pageSize, Integer status);

    OrderVO details(Long id);

    void userCancelById(Long id) throws Exception;

    void repetition(Long id);

    void reminder(Long id);

    void paySuccess(String outTradeNo);

    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    void delivery(Long id);

    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    void confirm(OrdersCancelDTO ordersCancelDTO);

    OrderStatisticsVO statistics();

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    void complete(Long id);
}
