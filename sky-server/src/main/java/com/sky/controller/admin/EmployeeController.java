package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.BeanUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/employee")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "员工相关接口")
public class EmployeeController {

    private final EmployeeService employeeService;

    private final JwtProperties jwtProperties;

    @Operation(summary = "员工登录")
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO){
        log.info("员工登录：{}", employeeLoginDTO);
        Employee employee = employeeService.login(employeeLoginDTO);

        // 登录成功后，生成jwt令牌
        Map<String,Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);
        EmployeeLoginVO employeeLoginVO = new EmployeeLoginVO();
        BeanUtil.copyProperties(employee,employeeLoginVO);
        employeeLoginVO.setToken(token);

        return Result.success(employeeLoginVO);
    }

    @PostMapping("/logout")
    @Operation(summary = "员工退出登录")
    public Result<String> logout(){
        return Result.success();
    }

    @PostMapping
    @Operation(summary = "新增员工")
    public Result<String> save(@RequestBody EmployeeDTO employeeDTO){
        log.info("新增员工,{}",employeeDTO);
        Employee employee = new Employee();
        BeanUtil.copyProperties(employeeDTO,employee);
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        employee.setStatus(StatusConstant.ENABLE);
        employeeService.save(employee);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "员工分页查询")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO){
        log.info("员工分页查询，参数为：{}",employeePageQueryDTO);
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    @PostMapping("/status/{status}")
    @Operation(summary = "启用或禁用员工账号")
    public Result<String> startOrStop(@PathVariable Integer status,Long id){
        log.info("启用禁用员工账户:{},{}",status,id);
        Employee employee = employeeService.getById(id);
        employee.setStatus(status);
        employeeService.updateById(employee);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户信息")
    public Result<Employee> getById(@PathVariable Long id){
        log.info("根据ID查询用户信息，{}",id);
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }

    @PutMapping
    @Operation(summary = "编辑员工信息")
    public Result<String> update(@RequestBody EmployeeDTO employeeDTO){
        log.info("编辑员工信息:{}",employeeDTO);
        Employee employee = employeeService.getById(employeeDTO.getId());
        BeanUtil.copyProperties(employeeDTO,employee);
        employeeService.updateById(employee);
        return Result.success();
    }
}
