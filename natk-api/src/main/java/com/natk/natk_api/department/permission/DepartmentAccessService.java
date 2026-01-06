package com.natk.natk_api.department.permission;

import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.department.repository.DepartmentRepository;
import com.natk.natk_api.department.repository.DepartmentUserRepository;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFolderAccessRepository;
import com.natk.natk_api.exception.FileOrFolderNotFoundOrNoAccessException;
import com.natk.natk_api.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentAccessService {

    private final DepartmentRepository departmentRepo;
    private final DepartmentFolderAccessRepository accessRepo;
    private final DepartmentUserRepository accessDepartmentRepo;

    public DepartmentEntity getDepartmentOrThrow(UUID departmentId) {
        return departmentRepo.findById(departmentId)
                .orElseThrow(FileOrFolderNotFoundOrNoAccessException::new);
    }

    //TODO: добавить реализацию hasFolderAccess в контроллере
    public enum Permission {
        MANAGE, // управление папками/департаментом
        ACCESS  // базовый доступ
    }

    public boolean hasPermission(UserEntity user, UUID departmentId, Permission permission) {
        switch (permission) {
            case MANAGE -> {
                return canManage(user, departmentId);
            }
            case ACCESS -> {
                return hasAnyAccess(user, departmentId);
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Может ли пользователь управлять департаментом?
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
     * Есть ли доступ к департаменту?
     * - ADMIN / DEPARTMENT_HEAD → доступ всегда
     * - иначе ищем явный доступ в department_folder_access
     */
    public boolean hasAnyAccess(UserEntity user, UUID departmentId) {
        return accessDepartmentRepo.existsByUserIdAndDepartmentId(user.getId(), departmentId)
                || canManage(user, departmentId);
    }

    /**
     * Есть ли доступ к конкретной папке?
     * - публичная папка → доступ у всех
     * - ADMIN / DEPARTMENT_HEAD → доступ всегда
     * - иначе ищем явный доступ в department_folder_access
     */
    public boolean hasFolderAccess(UserEntity user, DepartmentFolderEntity folder) {
        if (folder.isPublic()) return true;
        return canManage(user, folder.getDepartment().getId()) ||
                accessRepo.existsByUserIdAndFolderId(user.getId(), folder.getId());
    }
}
