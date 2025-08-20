package com.natk.natk_api.department;

import com.natk.natk_api.auth.CustomUserDetails;
import com.natk.natk_api.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("departmentSecurity")
@RequiredArgsConstructor
public class DepartmentSecurity {

    private final DepartmentRepository departmentRepository;

    /**
     * Проверка, что пользователь является начальником отдела
     */
    public boolean isChief(Authentication authentication, UUID departmentId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return false;
        }

        UUID userId = details.getId();
        return departmentRepository.findById(departmentId)
                .map(dept -> dept.getChiefId() != null && dept.getChiefId().equals(userId))
                .orElse(false);
    }
}
