package com.natk.natk_api.department.permission;

import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DepartmentPermissionEvaluator implements PermissionEvaluator {

    private final DepartmentAccessService departmentAccessService;
    private final CurrentUserService currentUserService;

    @Override
    public boolean hasPermission(Authentication authentication,
                                 Object targetDomainObject,
                                 Object permission) {
        if (!(authentication.getPrincipal() instanceof UserEntity user)) {
            return false;
        }
        if (targetDomainObject instanceof UUID departmentId &&
                permission instanceof String permStr) {
            DepartmentAccessService.Permission perm =
                    DepartmentAccessService.Permission.valueOf(permStr.toUpperCase());
            return departmentAccessService.hasPermission(user, departmentId, perm);
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication,
                                 Serializable targetId,
                                 String targetType,
                                 Object permission) {
        UserEntity user = currentUserService.getCurrentUser();
        if (targetId instanceof UUID departmentId &&
                permission instanceof String permStr &&
                "DEPARTMENT".equalsIgnoreCase(targetType)) {
            DepartmentAccessService.Permission perm =
                    DepartmentAccessService.Permission.valueOf(permStr.toUpperCase());
            return departmentAccessService.hasPermission(user, departmentId, perm);
        }
        return false;
    }
}
