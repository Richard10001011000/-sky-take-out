package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import com.sky.mapper.EmployeeMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.message.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

/**
* @author 12548
* @description 针对表【employee(员工信息)】的数据库操作Service实现
* @createDate 2024-08-28 12:48:55
*/
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
    implements EmployeeService{

    private final EmployeeMapper employeeMapper;

    @Override
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employeeLoginDTO.getUsername());
        Employee employee = employeeMapper.selectOne(queryWrapper);
        if(employee == null){
            throw  new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if(!employee.getPassword().equals(DigestUtils.md5DigestAsHex(employeeLoginDTO.getPassword().getBytes()))){
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if(StatusConstant.DISABLE.equals(employee.getStatus())){
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        return employee;
    }

    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // 使用 MyBatis-Plus 提供的分页功能
        Page<Employee> page = new Page<>(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(employeePageQueryDTO.getName()),Employee::getName,employeePageQueryDTO.getName());
        IPage<Employee> pageResult = employeeMapper.selectPage(page,queryWrapper);

        long total = pageResult.getTotal();
        List<Employee> records = pageResult.getRecords();

        return new PageResult(total,records);
    }
}




