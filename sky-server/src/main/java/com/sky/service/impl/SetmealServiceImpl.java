package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.mapper.SetmealMapper;
import com.sky.utils.BeanUtil;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author 12548
* @description 针对表【setmeal(套餐)】的数据库操作Service实现
* @createDate 2024-08-28 12:49:01
*/
@Service
@RequiredArgsConstructor
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal>
    implements SetmealService{

    private final SetmealMapper setmealMapper;

    private final SetmealDishMapper setmealDishMapper;
    private final DishMapper dishMapper;

    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtil.copyProperties(setmealDTO, setmeal);

        setmealMapper.insert(setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
        setmealDishMapper.insert(setmealDishes);
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        Page<SetmealVO> page = new Page<>(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        MPJLambdaWrapper<Setmeal> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(Setmeal.class).selectAs(Category::getName,SetmealVO::getCategoryName)
                .leftJoin(Category.class,Category::getId,Setmeal::getCategoryId)
                .like(StringUtils.isNotBlank(setmealPageQueryDTO.getName()), Setmeal::getName,setmealPageQueryDTO.getName())
                .eq(setmealPageQueryDTO.getCategoryId()!=null,Setmeal::getCategoryId,setmealPageQueryDTO.getCategoryId())
                .eq(setmealPageQueryDTO.getStatus()!=null,Setmeal::getStatus,setmealPageQueryDTO.getStatus());
        IPage<SetmealVO> pageResult = setmealMapper.selectJoinPage(page,SetmealVO.class,wrapper);
        return new PageResult(pageResult.getTotal(),pageResult.getRecords());
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        ids.forEach(id -> {
            Setmeal setmeal = setmealMapper.selectById(id);
            if(StatusConstant.ENABLE.equals(setmeal.getStatus())){
                throw  new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });
        setmealMapper.deleteByIds(ids);
        setmealDishMapper.delete(new LambdaQueryWrapper<SetmealDish>().in(SetmealDish::getSetmealId,ids));
    }

    @Override
    public void updateWithSetmealDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtil.copyProperties(setmealDTO,setmeal);
        setmealMapper.updateById(setmeal);

        setmealDishMapper.delete(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId,setmeal.getId()));
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));

        setmealDishMapper.insert(setmealDishes);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        if (StatusConstant.ENABLE.equals(status)) {
            List<SetmealDish> setmealDishes = setmealDishMapper.selectList(new LambdaQueryWrapper<SetmealDish>()
                    .eq(SetmealDish::getSetmealId, id));
            if (!CollectionUtils.isEmpty(setmealDishes)) {
                List<Long> dishIds = setmealDishes.stream().map(SetmealDish::getDishId).collect(Collectors.toList());
                List<Dish> dishes = dishMapper.selectBatchIds(dishIds);
                if (!CollectionUtils.isEmpty(dishes)) {
                    dishes.forEach(dish -> {
                        if (StatusConstant.DISABLE.equals(dish.getStatus())) {
                            throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                        }
                    });
                }
            }
            Setmeal setmeal = setmealMapper.selectById(id);
            setmeal.setStatus(status);
            setmealMapper.updateById(setmeal);
        }
    }

    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        MPJLambdaWrapper<SetmealDish> mpjLambdaWrapper = new MPJLambdaWrapper();
        mpjLambdaWrapper.select(SetmealDish::getName,SetmealDish::getCopies).select(Dish::getImage,Dish::getDescription)
                .leftJoin(Dish.class,Dish::getId,SetmealDish::getDishId).eq(SetmealDish::getId,id);
        List<DishItemVO> list = setmealDishMapper.selectJoinList(DishItemVO.class,mpjLambdaWrapper);
        return list;
    }

}




