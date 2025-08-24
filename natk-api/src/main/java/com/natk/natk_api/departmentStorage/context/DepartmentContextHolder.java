package com.natk.natk_api.departmentStorage.context;

import java.util.UUID;

public class DepartmentContextHolder {
    private static final ThreadLocal<UUID> context = new ThreadLocal<>();

    public static void set(UUID departmentId) {
        context.set(departmentId);
    }

    public static UUID get() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }
}
