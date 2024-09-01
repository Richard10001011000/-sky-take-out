package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 12548
* @description 针对表【shopping_cart(购物车)】的数据库操作Service
* @createDate 2024-08-28 12:49:04
*/
public interface ShoppingCartService extends IService<ShoppingCart> {

    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);

    void cleanShoppingCart();

    List<ShoppingCart> showShoppingCart();

    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
