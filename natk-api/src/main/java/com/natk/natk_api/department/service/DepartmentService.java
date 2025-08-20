package com.natk.natk_api.department.service;

import com.natk.natk_api.department.UserInDepartmentMapper;
import com.natk.natk_api.department.dto.AddDepartmentUserDto;
import com.natk.natk_api.department.dto.CreateDepartmentDto;
import com.natk.natk_api.department.dto.DepartmentDto;
import com.natk.natk_api.department.dto.DepartmentUserDto;
import com.natk.natk_api.department.dto.UpdateDepartmentDto;
import com.natk.natk_api.department.dto.UserInDepartmentDto;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.department.model.DepartmentUserEntity;
import com.natk.natk_api.department.repository.DepartmentRepository;
import com.natk.natk_api.department.repository.DepartmentUserRepository;
import com.natk.natk_api.users.model.RoleEntity;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.model.UserRoleEntity;
import com.natk.natk_api.users.repository.RoleRepository;
import com.natk.natk_api.users.repository.UserRepository;
import com.natk.natk_api.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final DepartmentUserRepository departmentUserRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public DepartmentDto createDepartment(CreateDepartmentDto dto) {
        UserEntity chiefEntity = userRepository.findById(dto.chiefId())
                .orElseThrow(() -> new IllegalArgumentException("Chief not found"));

        DepartmentEntity entity = DepartmentEntity.builder()
                .id(UUID.randomUUID())
                .name(dto.name())
                .chief(chiefEntity)
                .build();

        departmentRepository.save(entity);

        syncChiefRole(dto.chiefId());

        return new DepartmentDto(entity.getId(), entity.getName(),
                UserInDepartmentMapper.toUserInDepartmentDto(chiefEntity));
    }

    @Transactional
    public DepartmentDto updateDepartment(UUID id, UpdateDepartmentDto dto) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        boolean updated = false;

        if (dto.name() != null && !dto.name().equals(entity.getName())) {
            entity.setName(dto.name());
            updated = true;
        }

        if (dto.chiefId() != null) {
            UserEntity newChief = userRepository.findById(dto.chiefId())
                    .orElseThrow(() -> new IllegalArgumentException("Chief not found"));

            UUID oldChiefId = entity.getChief() != null ? entity.getChief().getId() : null;

            if (oldChiefId == null || !oldChiefId.equals(newChief.getId())) {
                entity.setChief(newChief);
                updated = true;

                if (oldChiefId != null) removeChiefRoleIfNoOtherDepartments(oldChiefId);
                syncChiefRole(newChief.getId());
            }
        }

        if (!updated) {
            throw new IllegalArgumentException("No changes detected for update");
        }

        departmentRepository.save(entity);

        return new DepartmentDto(entity.getId(), entity.getName(),
                UserInDepartmentMapper.toUserInDepartmentDto(entity.getChief()));
    }

    @Transactional
    public void deleteDepartment(UUID id) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        UUID chiefId = entity.getChief().getId();

        departmentRepository.deleteById(id);

        if (chiefId != null) {
            removeChiefRoleIfNoOtherDepartments(chiefId);
        }
    }

    public List<DepartmentDto> listDepartments() {
        return departmentRepository.findAllWithChief();
    }

    public List<DepartmentUserDto> listDepartmentUsers(UUID departmentId) {
        return departmentUserRepository.findAllByDepartmentId(departmentId);
    }

    public UUID getDepartmentIdByDepartmentUserId(UUID departmentUserId) {
        DepartmentUserEntity du = departmentUserRepository.findById(departmentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Department user not found"));
        return du.getDepartment().getId();
    }

    @Transactional
    public DepartmentUserDto addUserToDepartment(AddDepartmentUserDto dto) {
        UserEntity user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        DepartmentEntity department = departmentRepository.findById(dto.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        DepartmentUserEntity entity = DepartmentUserEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .department(department)
                .build();
        departmentUserRepository.save(entity);

        return new DepartmentUserDto(entity.getId(),
                UserInDepartmentMapper.toUserInDepartmentDto(user),
                department.getId());
    }

    public void removeUserFromDepartment(UUID id) {
        departmentUserRepository.deleteById(id);
    }

    private void syncChiefRole(UUID chiefId) {
        if (chiefId == null) return;

        RoleEntity role = roleRepository.findByName("DEPARTMENT_HEAD")
                .orElseThrow(() -> new IllegalStateException("Role DEPARTMENT_HEAD not found"));

        boolean hasRole = userRoleRepository.findByUserId(chiefId).stream()
                .anyMatch(r -> r.getRoleId().equals(role.getId()));

        if (!hasRole) {
            userRoleRepository.save(new UserRoleEntity(chiefId, role.getId()));
        }
    }

    private void removeChiefRoleIfNoOtherDepartments(UUID userId) {
        RoleEntity role = roleRepository.findByName("DEPARTMENT_HEAD")
                .orElseThrow(() -> new IllegalStateException("Role DEPARTMENT_HEAD not found"));

        boolean stillChiefSomewhere = departmentRepository.existsByChiefId(userId);

        if (!stillChiefSomewhere) {
            userRoleRepository.deleteByUserIdAndRoleId(userId, role.getId());
        }
    }
}
