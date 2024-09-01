package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.entity.DishFlavor;
import com.sky.service.DishFlavorService;
import com.sky.mapper.DishFlavorMapper;
import org.springframework.stereotype.Service;

/**
* @author 12548
* @description 针对表【dish_flavor(菜品口味关系表)】的数据库操作Service实现
* @createDate 2024-08-28 12:48:53
*/
@Service
public class DishFlavorServiceImpl extends ServiceImpl<DishFlavorMapper, DishFlavor>
    implements DishFlavorService{

}




