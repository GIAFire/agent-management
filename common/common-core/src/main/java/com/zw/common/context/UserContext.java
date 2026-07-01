package com.zw.common.context;


public class UserContext {

    private static final ThreadLocal<UserInfo> USER_THREAD_LOCAL = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserInfo loginUser) {
        USER_THREAD_LOCAL.set(loginUser);
    }

    public static UserInfo get() {
        return USER_THREAD_LOCAL.get();
    }

    public static Long getUserId() {
        UserInfo loginUser = get();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static String getUsername() {
        UserInfo loginUser = get();
        return loginUser == null ? null : loginUser.getUserName();
    }

    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }
}