package com.sky.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.yulichang.base.MPJBaseMapper;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.Map;

/**
* @author 12548
* @description 针对表【setmeal(套餐)】的数据库操作Mapper
* @createDate 2024-08-28 12:49:01
* @Entity com.sky.entity.Setmeal
*/
public interface SetmealMapper extends MPJBaseMapper<Setmeal> {

    default Integer countByMap(Map map){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(map.get("status")!=null,Setmeal::getStatus,map.get("status"))
                .eq(map.get("categoryId")!=null,Setmeal::getCategoryId,map.get("categoryId"));
        return selectCount(queryWrapper).intValue();
    }
}




