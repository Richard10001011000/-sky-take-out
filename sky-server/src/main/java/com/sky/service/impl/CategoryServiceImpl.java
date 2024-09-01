package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Employee;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 12548
* @description 针对表【category(菜品及套餐分类)】的数据库操作Service实现
* @createDate 2024-08-28 12:48:49
*/
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
    implements CategoryService{

    private final CategoryMapper categoryMapper;

    @Override
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        // 使用 MyBatis-Plus 提供的分页功能
        Page<Category> page = new Page<>(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(categoryPageQueryDTO.getType()!=null,Category::getType, categoryPageQueryDTO.getType())
                .like(StringUtils.isNotBlank(categoryPageQueryDTO.getName()), Category::getName, categoryPageQueryDTO.getName())
                .orderByAsc(Category::getSort).orderByDesc(Category::getCreateTime);
        IPage<Category> pageResult = categoryMapper.selectPage(page,queryWrapper);

        long total = pageResult.getTotal();
        List<Category> records = pageResult.getRecords();

        return new PageResult(total,records);
    }
}




