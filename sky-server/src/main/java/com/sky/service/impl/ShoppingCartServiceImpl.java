package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.ShoppingCartService;
import com.sky.mapper.ShoppingCartMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author 12548
* @description 针对表【shopping_cart(购物车)】的数据库操作Service实现
* @createDate 2024-08-28 12:49:04
*/
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart>
    implements ShoppingCartService{

    private final ShoppingCartMapper shoppingCartMapper;

    private final DishMapper dishMapper;

    private SetmealMapper setmealMapper;

    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

//        只能查询自己的购物车数据
        shoppingCart.setUserId(BaseContext.getCurrentId());

//        判断当前商品是否在购物车中
        List<ShoppingCart> shoppingCartsList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartsList != null && shoppingCartsList.size() > 0) {
//            如果存在，就更新数量，+1
            shoppingCart = shoppingCartsList.get(0);
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            shoppingCartMapper.updateById(shoppingCart);
        } else {
//            如果不存在，插入数据
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
//                添加到购物车的是菜品
                Dish dish = dishMapper.selectById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
//                添加到购物车的是套餐
                Setmeal setmeal = setmealMapper.selectById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

@Override
public List<ShoppingCart> showShoppingCart() {
    return shoppingCartMapper.list(ShoppingCart.builder().userId(BaseContext.getCurrentId()).build());
}

/**
 * 清空购物车商品
 */
@Override
public void cleanShoppingCart() {
    shoppingCartMapper.delete(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId,BaseContext.getCurrentId()));
}

/**
 * 删除购物车中一个商品
 * @param shoppingCartDTO
 */
@Override
public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
    ShoppingCart shoppingCart = new ShoppingCart();
    BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

//        设置查询条件，查询当前登录用户的购物车数据
    shoppingCart.setUserId(BaseContext.getCurrentId());

    List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
    if (list != null && list.size() > 0) {
        shoppingCart = list.get(0);

        Integer number = shoppingCart.getNumber();
        if (number == 1) {
//                当前商品在购物车中份数为1，直接删除当前记录
            shoppingCartMapper.deleteById(shoppingCart.getId());
        } else {
//                当前商品在购物车中的份数不为1，修改份数即可
            shoppingCart.setNumber(shoppingCart.getNumber() - 1);
            shoppingCartMapper.updateById(shoppingCart);
        }
    }
}
}




