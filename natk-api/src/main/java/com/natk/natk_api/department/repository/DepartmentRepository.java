package com.natk.natk_api.department.repository;

import com.natk.natk_api.department.dto.DepartmentDto;
import com.natk.natk_api.department.model.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {
    @Query("""
        select new com.natk.natk_api.department.dto.DepartmentDto(
            d.id,
            d.name,
            new com.natk.natk_api.department.dto.UserInDepartmentDto(
                u.id, u.name, u.surname, u.patronymic
            )
        )
        from DepartmentEntity d
        left join UserEntity u on d.chiefId = u.id
    """)
    List<DepartmentDto> findAllWithChief();
    boolean existsByChiefId(UUID chiefId);
}
