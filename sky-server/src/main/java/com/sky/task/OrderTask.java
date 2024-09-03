package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderTask {

    private final OrdersMapper ordersMapper;

    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){
        log.info("定时处理超时订单:{}", LocalDateTime.now());

        List<Orders> ordersList = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getPayStatus,Orders.PENDING_PAYMENT)
                .lt(Orders::getOrderTime,LocalDateTime.now()));

        if (CollectionUtils.isEmpty(ordersList)){
            return;
        }
        for (Orders orders : ordersList) {
            orders.setStatus(Orders.CANCELLED);
            orders.setCancelReason("订单超时，自动取消");
            orders.setCancelTime(LocalDateTime.now());
            ordersMapper.updateById(orders);
        }
    }

    @Scheduled(cron = "0 0 1 * * ?") //每天凌晨1点触发
    public void processDeliveryOrder(){
        log.info("定时处理派送中订单:{}", LocalDateTime.now());

        List<Orders> ordersList = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getPayStatus,Orders.DELIVERY_IN_PROGRESS)
                .lt(Orders::getOrderTime,LocalDateTime.now().plusMinutes(-60)));
        if (CollectionUtils.isEmpty(ordersList)){
            return;
        }
        for (Orders orders : ordersList) {
            orders.setStatus(Orders.COMPLETED);
            ordersMapper.updateById(orders);
        }
    }

}
