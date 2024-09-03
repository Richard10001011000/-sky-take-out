package com.sky.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.utils.BeanUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
* @author 12548
* @description 针对表【orders(订单表)】的数据库操作Mapper
* @createDate 2024-08-28 12:48:59
* @Entity com.sky.entity.Orders
*/
public interface OrdersMapper extends MPJBaseMapper<Orders> {

    default IPage<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO){
        Orders orders = new Orders();
        BeanUtil.copyProperties(ordersPageQueryDTO, orders);
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>(orders);
        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        return selectPage(page, queryWrapper);
    }

    default Orders getByNumberAndUserId(String outTradeNo, Long userId){
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getNumber,outTradeNo).eq(Orders::getUserId,userId);
        return selectOne(queryWrapper);
    }

    default Integer countStatus(Integer toBeConfirmed){
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getStatus,toBeConfirmed);
        return selectCount(queryWrapper).intValue();
    }

    default Double sumByMap(Map map){
        MPJLambdaWrapper<Orders> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectSum(Orders::getAmount).eq(map.get("status")!=null,Orders::getStatus,map.get("status"))
                .ge(map.get("begin")!=null,Orders::getOrderTime,map.get("begin"))
                .le(map.get("end")!=null,Orders::getOrderTime,map.get("end"));
        Double sum = selectJoinOne(Double.class,wrapper);
        return sum!=null?sum:0.0;
    }

    default Integer countByMap(Map map){
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(map.get("status")!=null,Orders::getStatus,map.get("status"))
                .ge(map.get("begin")!=null,Orders::getOrderTime,map.get("begin"))
                .le(map.get("end")!=null,Orders::getOrderTime,map.get("end"));
        return selectCount(queryWrapper).intValue();
    }

    List<GoodsSalesDTO> getSalesTop10(LocalDateTime beginTime, LocalDateTime endTime);
}




