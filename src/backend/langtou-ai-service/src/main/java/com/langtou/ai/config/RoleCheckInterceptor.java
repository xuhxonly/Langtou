package com.langtou.ai.config;

import com.langtou.common.annotation.RequireRole;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class RoleCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            return true;
        }
        String userId = request.getHeader(CommonConstants.REQUEST_USER_ID);
        if (userId == null || userId.isEmpty()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录");
        }
        return true;
    }
}