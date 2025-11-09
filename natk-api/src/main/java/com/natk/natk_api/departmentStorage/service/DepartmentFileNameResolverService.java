package com.natk.natk_api.departmentStorage.service;

import com.natk.natk_api.baseStorage.service.AbstractFileNameResolverService;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFileEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentFileNameResolverService extends AbstractFileNameResolverService<DepartmentFolderEntity, DepartmentEntity> {

    private final DepartmentFileRepository fileRepo;

    @Override
    protected Set<String> getExistingFileNames(DepartmentFolderEntity parentFolder, DepartmentEntity dept, UUID excludeFileId) {
        List<DepartmentFileEntity> files;
        if (parentFolder == null) {
            files = fileRepo.findByDepartmentAndFolderIsNullAndIsDeletedFalse(dept);
        } else {
            files = fileRepo.findByDepartmentAndFolderAndIsDeletedFalse(dept, parentFolder);
        }

        return files.stream()
                .filter(f -> !Objects.equals(f.getId(), excludeFileId))
                .map(DepartmentFileEntity::getName)
                .collect(Collectors.toSet());
    }
}
