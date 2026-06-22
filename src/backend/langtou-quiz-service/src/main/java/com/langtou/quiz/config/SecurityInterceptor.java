package com.langtou.quiz.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SecurityInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        try {
            Long.parseLong(userIdHeader);
            return true;
        } catch (NumberFormatException e) {
            response.setStatus(401);
            return false;
        }
    }
}
