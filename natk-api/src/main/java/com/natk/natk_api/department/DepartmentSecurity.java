package com.natk.natk_api.department;

import com.natk.natk_api.auth.CustomUserDetails;
import com.natk.natk_api.department.repository.DepartmentRepository;
import com.natk.natk_api.departmentStorage.context.DepartmentContextHolder;
import com.natk.natk_api.departmentStorage.repository.DepartmentFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("departmentSecurity")
@RequiredArgsConstructor
public class DepartmentSecurity {

    private final DepartmentRepository departmentRepository;
    private final DepartmentFolderRepository folderRepository;

    /**
     * Проверка, что пользователь является начальником отдела
     */
    public boolean isChief(Authentication authentication, UUID departmentId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return false;
        }

        UUID userId = details.getId();
        return departmentRepository.existsByIdAndChiefId(departmentId, userId);
    }

    public boolean hasAccess(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return false;
        }

        UUID userId = details.getId();
        UUID departmentId = DepartmentContextHolder.get();
        if (departmentId == null) return false;

        // ADMIN всегда имеет доступ
        boolean isAdmin = details.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;

        // проверка, что пользователь является начальником департамента
        return departmentRepository.existsByIdAndChiefId(departmentId, userId);
    }
}
