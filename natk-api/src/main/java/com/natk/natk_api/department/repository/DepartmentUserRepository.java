package com.natk.natk_api.department.repository;

import com.natk.natk_api.department.dto.DepartmentUserDto;
import com.natk.natk_api.department.model.DepartmentUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DepartmentUserRepository extends JpaRepository<DepartmentUserEntity, UUID> {
    @Query("""
        select new com.natk.natk_api.department.dto.DepartmentUserDto(
            du.id,
            new com.natk.natk_api.department.dto.UserInDepartmentDto(
                u.id, u.name, u.surname, u.patronymic
            ),
            d.id
        )
        from DepartmentUserEntity du
        join du.user u
        join du.department d
        where d.id = :departmentId
    """)
    List<DepartmentUserDto> findAllByDepartmentId(UUID departmentId);
    boolean existsByUserIdAndDepartmentId(UUID userId, UUID departmentId);
}
