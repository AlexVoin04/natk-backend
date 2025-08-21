package com.natk.natk_api.department.service;

import com.natk.natk_api.department.UserInDepartmentMapper;
import com.natk.natk_api.department.dto.AddDepartmentUserDto;
import com.natk.natk_api.department.dto.AddDepartmentUsersDto;
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

import java.util.ArrayList;
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

        updated |= updateNameIfChanged(entity, dto.name());
        updated |= updateChiefIfChanged(entity, dto.chiefId());

        if (!updated) {
            throw new IllegalArgumentException("No changes detected for update");
        }

        departmentRepository.save(entity);

        return new DepartmentDto(entity.getId(), entity.getName(),
                UserInDepartmentMapper.toUserInDepartmentDto(entity.getChief()));
    }

    private boolean updateNameIfChanged(DepartmentEntity entity, String newName) {
        if (newName != null) {
            if (newName.isBlank()) {
                throw new IllegalArgumentException("The department name cannot be empty");
            }
            if (!newName.equals(entity.getName())) {
                entity.setName(newName);
                return true;
            }
        }
        return false;
    }

    private boolean updateChiefIfChanged(DepartmentEntity entity, UUID newChiefId) {
        if (newChiefId == null) return false;

        UserEntity newChief = userRepository.findById(newChiefId)
                .orElseThrow(() -> new IllegalArgumentException("Chief not found"));

        UUID oldChiefId = entity.getChief() != null ? entity.getChief().getId() : null;

        if (oldChiefId == null || !oldChiefId.equals(newChiefId)) {
            entity.setChief(newChief);

            if (oldChiefId != null) {
                removeChiefRoleIfNoOtherDepartments(oldChiefId);
            }
            syncChiefRole(newChiefId);

            return true;
        }
        return false;
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

    public List<UserInDepartmentDto> listUsersNotInDepartment(UUID departmentId) {
        List<UserEntity> users = userRepository.findUsersNotInDepartment(departmentId);
        return users.stream()
                .map(UserInDepartmentMapper::toUserInDepartmentDto)
                .toList();
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

        return addUserToDepartmentInternal(user, department, false);
    }

    @Transactional
    public List<DepartmentUserDto> addUsersToDepartment(AddDepartmentUsersDto dto) {
        DepartmentEntity department = departmentRepository.findById(dto.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        List<DepartmentUserDto> result = new ArrayList<>();

        for (UUID userId : dto.userIds()) {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            DepartmentUserDto added = addUserToDepartmentInternal(user, department, true);
            if (added != null) {
                result.add(added);
            }
        }

        return result;
    }

    private DepartmentUserDto addUserToDepartmentInternal(UserEntity user, DepartmentEntity department, boolean ignoreIfExists) {
        boolean exists = departmentUserRepository.existsByUserIdAndDepartmentId(user.getId(), department.getId());
        if (exists) {
            if (ignoreIfExists) {
                return null;
            } else {
                throw new IllegalArgumentException("User " + user.getId() + " is already in department " + department.getId());
            }
        }

        DepartmentUserEntity entity = DepartmentUserEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .department(department)
                .build();

        departmentUserRepository.save(entity);

        return new DepartmentUserDto(
                entity.getId(),
                UserInDepartmentMapper.toUserInDepartmentDto(user),
                department.getId()
        );
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
