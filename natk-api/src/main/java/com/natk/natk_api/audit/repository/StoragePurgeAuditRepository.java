package com.natk.natk_api.audit.repository;

import com.natk.natk_api.audit.model.StoragePurgeAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoragePurgeAuditRepository
        extends JpaRepository<StoragePurgeAuditEntity, UUID> {
}