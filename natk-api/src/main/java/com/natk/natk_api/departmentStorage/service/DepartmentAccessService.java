package com.natk.natk_api.departmentStorage.service;

import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.department.repository.DepartmentRepository;
import com.natk.natk_api.departmentStorage.context.DepartmentContextHolder;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFolderAccessRepository;
import com.natk.natk_api.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentAccessService {

    private final DepartmentRepository departmentRepo;
    private final DepartmentFolderAccessRepository accessRepo;

    public DepartmentEntity getDepartmentOrThrow(UUID departmentId) {
        return departmentRepo.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
    }

    /**
     * Может ли пользователь управлять всеми папками департамента?
     * ADMIN и DEPARTMENT_HEAD имеют полный доступ
     */
    public boolean canManage(UserEntity user, UUID departmentId) {
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"))) {
            return true;
        }

        DepartmentEntity dept = getDepartmentOrThrow(departmentId);
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("DEPARTMENT_HEAD"))) {
            return dept.getChief().getId().equals(user.getId());
        }

        return false;
    }

    /**
     * Есть ли доступ к конкретной папке?
     * - публичная папка → доступ у всех
     * - ADMIN / DEPARTMENT_HEAD → доступ всегда
     * - иначе ищем явный доступ в department_folder_access
     */
    public boolean hasAccess(UserEntity user, DepartmentFolderEntity folder) {
        if (folder.isPublic()) {
            return true;
        }
        if (canManage(user, folder.getDepartment().getId())) {
            return true;
        }
        return accessRepo.existsByUserIdAndFolderId(user.getId(), folder.getId());
    }

    public UUID getCurrentDepartmentId() {
        UUID depId = DepartmentContextHolder.get();
        if (depId == null) {
            throw new IllegalStateException("DepartmentId is missing in request context");
        }
        return depId;
    }
}
