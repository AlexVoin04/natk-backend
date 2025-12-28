package com.natk.natk_api.audit.service;

import com.natk.natk_api.audit.enums.PurgeAuditType;
import com.natk.natk_api.audit.enums.StorageScope;
import com.natk.natk_api.audit.model.StoragePurgeAuditEntity;
import com.natk.natk_api.audit.repository.StoragePurgeAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoragePurgeAuditService {

    private final StoragePurgeAuditRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserPurge(
            UUID userId,
            UUID targetId,
            PurgeAuditType targetType,
            int files,
            int folders
    ) {
        repo.save(StoragePurgeAuditEntity.builder()
                .userId(userId)
                .targetId(targetId)
                .targetType(targetType)
                .storage(StorageScope.USER)
                .purgedAt(Instant.now())
                .filesDeleted(files)
                .foldersDeleted(folders)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDepartmentPurge(
            UUID userId,
            UUID departmentId,
            UUID targetId,
            PurgeAuditType targetType,
            int files,
            int folders
    ) {
        repo.save(StoragePurgeAuditEntity.builder()
                .userId(userId)
                .departmentId(departmentId)
                .targetId(targetId)
                .targetType(targetType)
                .storage(StorageScope.DEPARTMENT)
                .purgedAt(Instant.now())
                .filesDeleted(files)
                .foldersDeleted(folders)
                .build());
    }
}