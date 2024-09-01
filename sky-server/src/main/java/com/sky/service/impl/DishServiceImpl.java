package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.query.MPJLambdaQueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.*;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.mapper.DishMapper;
import com.sky.utils.BeanUtil;
import com.sky.vo.DishVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
* @author 12548
* @description 针对表【dish(菜品)】的数据库操作Service实现
* @createDate 2024-08-28 12:48:51
*/
@Service
@RequiredArgsConstructor
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish>
    implements DishService{

    private final DishMapper dishMapper;

    private final DishFlavorMapper dishFlavorMapper;

    private final SetmealDishMapper setmealDishMapper;

    private final SetmealMapper setmealMapper;
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //        判断当前菜品是否能够删除---是否存在起售中的菜品？？
        ids.forEach(id->{
            Dish dish = dishMapper.selectById(id);
            if (StatusConstant.ENABLE.equals(dish.getStatus())) {
//                当前菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        });

//        判断当前菜品是否能够删除---是否被套餐关联了？？
        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(new LambdaQueryWrapper<SetmealDish>().in(SetmealDish::getDishId, ids));
        if (setmealDishes != null && setmealDishes.size() > 0) {
//            当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        dishMapper.deleteBatchIds(ids);
        dishFlavorMapper.delete(new LambdaQueryWrapper<DishFlavor>().in(DishFlavor::getDishId, ids));
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        Page<DishVO> page = new Page(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        MPJLambdaWrapper<Dish> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(Dish.class).selectAs(Category::getName, DishVO::getCategoryName).leftJoin(Category.class,Category::getId,Dish::getCategoryId)
                .like(StringUtils.isNotBlank(dishPageQueryDTO.getName()),Dish::getName,dishPageQueryDTO.getName())
                .eq(dishPageQueryDTO.getCategoryId()!=null,Dish::getCategoryId,dishPageQueryDTO.getCategoryId())
                .eq(dishPageQueryDTO.getStatus()!=null,Dish::getStatus,dishPageQueryDTO.getStatus());
        IPage<DishVO> pageResult = dishMapper.selectJoinPage(page,DishVO.class,wrapper);
        return  new PageResult(pageResult.getTotal(),pageResult.getRecords());
    }

    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtil.copyProperties(dishDTO, dish);

        dishMapper.insert(dish);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dish.getId()));
            dishFlavorMapper.insert(flavors);
        }
    }

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.selectById(id);

        List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(new LambdaQueryWrapper<DishFlavor>()
                .eq(DishFlavor::getDishId, id));
        DishVO dishVO = new DishVO();
        BeanUtil.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtil.copyProperties(dishDTO, dish);
        dishMapper.updateById(dish);
        dishFlavorMapper.delete(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, dishDTO.getId()));
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dish.getId()));
            dishFlavorMapper.insert(flavors);
        }
    }

    @Override
    public List<Dish> listByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus, StatusConstant.ENABLE);
        return dishMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = dishMapper.selectById(id);
        dish.setStatus(status);
        dishMapper.updateById(dish);

        // 如果禁用菜品包含该菜品的套餐也得停售
        if(StatusConstant.DISABLE.equals(status)) {
            List<SetmealDish> setmealDishes = setmealDishMapper.selectList(new LambdaQueryWrapper<SetmealDish>()
                    .eq(SetmealDish::getDishId,id));
            if (setmealDishes!=null && setmealDishes.size()>0) {
                List<Long> ids = setmealDishes.stream().map(SetmealDish::getDishId).collect(Collectors.toList());
                List<Setmeal> setmeals = setmealMapper.selectBatchIds(ids);
                setmeals.forEach(setmeal -> setmeal.setStatus(StatusConstant.DISABLE));
                setmealMapper.updateById(setmeals);
            }
        }
    }

    @Override
    public List<DishVO> listWithFlavor(Long categoryId) {
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>().eq(Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus, StatusConstant.ENABLE));
        ArrayList<DishVO> dishVOs = new ArrayList<>();

        dishes.forEach(dish -> {
            DishVO dishVO = new DishVO();
            BeanUtil.copyProperties(dish, dishVO);
            List<DishFlavor> flavors = dishFlavorMapper.selectList(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId,dish.getId()));
            dishVO.setFlavors(flavors);
            dishVOs.add(dishVO);
        });
        return dishVOs;
    }
}




