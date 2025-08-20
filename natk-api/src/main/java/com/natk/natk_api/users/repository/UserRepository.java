package com.natk.natk_api.users.repository;

import com.natk.natk_api.users.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    @Query(value = "SELECT DISTINCT u FROM UserEntity u JOIN u.roles r " +
            "WHERE (:role IS NULL OR :role = '' OR r.name = :role) " +
            "AND (:name IS NULL OR :name = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:surname IS NULL OR :surname = '' OR LOWER(u.surname) LIKE LOWER(CONCAT('%', :surname, '%')))",
            countQuery = "SELECT COUNT(DISTINCT u) FROM UserEntity u JOIN u.roles r " +
                    "WHERE (:role IS NULL OR :role = '' OR r.name = :role) " +
                    "AND (:name IS NULL OR :name = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
                    "AND (:surname IS NULL OR :surname = '' OR LOWER(u.surname) LIKE LOWER(CONCAT('%', :surname, '%')))")
    Page<UserEntity> findByFilters(@Param("role") String role,
                                   @Param("name") String name,
                                   @Param("surname") String surname,
                                   Pageable pageable);
}

