package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.PasswordConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

    @PostMapping
    @ApiOperation("新增员工")
    private Result save(@RequestBody EmployeeDTO employeeDTO){
        log.info("新增员工{}", employeeDTO);//{}是占位符，代码运行的时候会自动拼接
        employeeService.save(employeeDTO);
        return Result.success();
    }

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("员工分页查询")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO){
        log.info("员工分页查询");
        PageResult pageResult = employeeService.PageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("修改员工状态")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("启用禁用员工账号{}，{}", status, id);
        employeeService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 根据id查找员工
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查找员工")
    public Result<Employee> selectById(@PathVariable Long id){
        Employee employee = employeeService.selectById(id);
        return Result.success(employee);
    }

    /**
     * 更新员工信息
     * @param employeeDTO
     * @return
     */
    @PutMapping
    @ApiOperation("更新员工信息")
    public Result update(@RequestBody EmployeeDTO employeeDTO){
        employeeService.update(employeeDTO);
        return Result.success();
    }
}
