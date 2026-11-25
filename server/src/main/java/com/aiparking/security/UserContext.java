package com.aiparking.security;

import com.aiparking.common.UnauthorizedException;

/**
 * 当前登录用户上下文（请求级 ThreadLocal，拦截器 afterCompletion 清理）。
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static CurrentUser require() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        return user;
    }

    public static Long currentUserId() {
        return require().userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
