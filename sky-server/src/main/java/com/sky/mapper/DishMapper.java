package com.sky.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.yulichang.base.MPJBaseMapper;
import com.sky.entity.Dish;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;

import java.util.Map;

/**
* @author 12548
* @description 针对表【dish(菜品)】的数据库操作Mapper
* @createDate 2024-08-28 12:48:51
* @Entity com.sky.entity.Dish
*/
public interface DishMapper extends MPJBaseMapper<Dish> {

    default Integer countByMap(Map map){
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(map.get("status")!=null,Dish::getStatus,map.get("status"))
                .eq(map.get("categoryId")!=null,Dish::getCategoryId,map.get("categoryId"));

        return selectCount(queryWrapper).intValue();
    }
}




