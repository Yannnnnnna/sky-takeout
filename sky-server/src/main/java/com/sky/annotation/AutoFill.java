package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.*;

/**
 * @author wyr on 2025/5/12
 * 自定义注解，用于表示某个方法需要进行功能字段自动处理
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    //数据库操作类型：update insert
    OperationType value();
}
