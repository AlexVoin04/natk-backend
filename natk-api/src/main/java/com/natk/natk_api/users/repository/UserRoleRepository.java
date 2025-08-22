package com.natk.natk_api.users.repository;

import com.natk.natk_api.users.model.UserRoleEntity;
import com.natk.natk_api.users.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {
    List<UserRoleEntity> findByUserId(UUID userId);
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
