package com.langtou.content.config;

import com.langtou.common.annotation.RequireRole;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 角色权限校验切面
 *
 * 拦截标注了 @RequireRole 注解的 Controller 方法，
 * 从请求头中读取用户角色进行校验（纵深防御，与网关层权限校验互补）。
 */
@Slf4j
@Aspect
@Component
public class RoleCheckAspect {

    /**
     * 环绕通知：校验 @RequireRole 注解标注的方法
     */
    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("无法获取请求上下文，跳过权限校验");
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();

        // 1. 登录校验：userId 不能为空
        String userIdStr = request.getHeader(CommonConstants.REQUEST_USER_ID);
        if (userIdStr == null || userIdStr.isEmpty()) {
            log.warn("未登录用户访问受保护接口: uri={}", request.getRequestURI());
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 2. 角色校验：如果注解指定了角色，检查用户是否拥有对应角色
        String[] requiredRoles = requireRole.value();
        if (requiredRoles.length > 0) {
            String userRole = request.getHeader(CommonConstants.REQUEST_USER_ROLE);
            if (userRole == null || userRole.isEmpty()) {
                log.warn("用户角色信息缺失: uri={}, userId={}", request.getRequestURI(), userIdStr);
                throw new BusinessException(ResultCode.FORBIDDEN);
            }

            boolean hasRole = false;
            for (String role : requiredRoles) {
                if (role.equals(userRole)) {
                    hasRole = true;
                    break;
                }
            }

            if (!hasRole) {
                log.warn("用户角色权限不足: uri={}, userId={}, userRole={}, requiredRoles={}",
                        request.getRequestURI(), userIdStr, userRole, requiredRoles);
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        }

        return joinPoint.proceed();
    }

    /**
     * 环绕通知：校验类级别 @RequireRole 注解
     */
    @Around("@within(requireRole)")
    public Object checkClassRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        return checkRole(joinPoint, requireRole);
    }
}
