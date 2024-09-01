package com.sky.service;

import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.result.PageResult;

/**
* @author 12548
* @description 针对表【category(菜品及套餐分类)】的数据库操作Service
* @createDate 2024-08-28 12:48:49
*/
public interface CategoryService extends IService<Category> {

    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);
}
