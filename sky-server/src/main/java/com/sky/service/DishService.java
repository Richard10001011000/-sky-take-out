package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

/**
* @author 12548
* @description 针对表【dish(菜品)】的数据库操作Service
* @createDate 2024-08-28 12:48:51
*/
public interface DishService extends IService<Dish> {

    void deleteBatch(List<Long> ids);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void saveWithFlavor(DishDTO dishDTO);

    DishVO getByIdWithFlavor(Long id);

    void updateWithFlavor(DishDTO dishDTO);

    List<Dish> listByCategoryId(Long categoryId);

    void startOrStop(Integer status, Long id);

    List<DishVO> listWithFlavor(Long categoryId);
}
