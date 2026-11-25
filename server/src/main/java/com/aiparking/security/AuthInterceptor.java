package com.aiparking.security;

import com.aiparking.common.ForbiddenException;
import com.aiparking.common.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("未登录或登录已过期");
        }

        JwtPayload payload;
        try {
            payload = jwtService.parse(header.substring(BEARER_PREFIX.length()));
        } catch (Exception e) {
            throw new UnauthorizedException("登录凭证无效或已过期");
        }

        UserContext.set(new CurrentUser(payload.userId(), payload.username(), payload.roles()));

        RequireRole required = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (required == null) {
            required = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (required != null) {
            List<String> requiredRoles = Arrays.asList(required.value());
            boolean matched = payload.roles().stream().anyMatch(requiredRoles::contains);
            if (!matched) {
                throw new ForbiddenException("无权限访问该资源");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
