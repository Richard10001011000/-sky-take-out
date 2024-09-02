package com.sky.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.OrderDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author 12548
* @description 针对表【order_detail(订单明细表)】的数据库操作Mapper
* @createDate 2024-08-28 12:48:57
* @Entity com.sky.entity.OrderDetail
*/
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

    default List<OrderDetail> getByOrderId(Long id){
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, id);
        return selectList(queryWrapper);
    }
}




