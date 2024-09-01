package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;

import java.util.List;

/**
* @author 12548
* @description 针对表【setmeal(套餐)】的数据库操作Service
* @createDate 2024-08-28 12:49:01
*/
public interface SetmealService extends IService<Setmeal> {

    void saveWithDish(SetmealDTO setmealDTO);

    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void deleteBatch(List<Long> ids);

    void updateWithSetmealDish(SetmealDTO setmealDTO);

    void startOrStop(Integer status, Long id);

    List<DishItemVO> getDishItemById(Long id);
}
