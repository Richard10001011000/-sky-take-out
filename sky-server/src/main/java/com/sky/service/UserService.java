package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 12548
* @description 针对表【user(用户信息)】的数据库操作Service
* @createDate 2024-08-28 12:49:07
*/
public interface UserService extends IService<User> {

    User wxlogin(UserLoginDTO userLoginDTO);
}
