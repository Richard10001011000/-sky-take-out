package com.sky.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.Map;

/**
* @author 12548
* @description 针对表【user(用户信息)】的数据库操作Mapper
* @createDate 2024-08-28 12:49:07
* @Entity com.sky.entity.User
*/
public interface UserMapper extends BaseMapper<User> {

    default Integer countByMap(Map map){
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(map.get("begin")!=null,User::getCreateTime,map.get("begin"))
                .le(map.get("end")!=null,User::getCreateTime,map.get("end"));
        return selectCount(queryWrapper).intValue();
    }
}




