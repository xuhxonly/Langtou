package com.langtou.common.annotation;

import java.lang.annotation.*;

/**
 * 角色权限校验注解
 *
 * 用于 Controller 方法上，标注所需的访问角色。
 * 通过 AOP 切面从请求头中读取用户角色进行校验。
 *
 * 使用示例：
 * <pre>
 *     @RequireRole("ADMIN")   // 仅管理员可访问
 *     @RequireRole            // 默认需要登录（userId 不为空）
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 所需角色列表，默认为空表示仅需登录（不限制角色）
     */
    String[] value() default {};
}
