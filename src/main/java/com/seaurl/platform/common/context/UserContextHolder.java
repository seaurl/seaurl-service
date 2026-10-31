package com.seaurl.platform.common.context;

public final class UserContextHolder {
    private static final ThreadLocal<UserContext> CONTEXT = ThreadLocal.withInitial(UserContext::new);

    private UserContextHolder() {
    }

    public static UserContext getContext() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
