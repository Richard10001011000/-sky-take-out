package com.sky.service;

import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.result.PageResult;

/**
* @author 12548
* @description 针对表【employee(员工信息)】的数据库操作Service
* @createDate 2024-08-28 12:48:55
*/
public interface EmployeeService extends IService<Employee> {

    Employee login(EmployeeLoginDTO employeeLoginDTO);

    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

}
