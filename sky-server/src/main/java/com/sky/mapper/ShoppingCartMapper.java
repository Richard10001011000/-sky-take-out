package com.sky.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.ShoppingCart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author 12548
* @description 针对表【shopping_cart(购物车)】的数据库操作Mapper
* @createDate 2024-08-28 12:49:04
* @Entity com.sky.entity.ShoppingCart
*/
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

    default List<ShoppingCart> list(ShoppingCart shoppingCart){
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>(shoppingCart);
        queryWrapper.orderByDesc(ShoppingCart::getCreateTime);
        return selectList(queryWrapper);
    }

}




