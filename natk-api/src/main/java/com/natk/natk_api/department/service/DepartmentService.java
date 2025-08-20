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
        DepartmentEntity entity = new DepartmentEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(dto.name());
        entity.setChiefId(dto.chiefId());
        departmentRepository.save(entity);

        syncChiefRole(dto.chiefId());

        UserInDepartmentDto chief = null;
        if (dto.chiefId() != null) {
            UserEntity chiefEntity = userRepository.findById(dto.chiefId())
                    .orElseThrow(() -> new IllegalArgumentException("Chief not found"));
            chief = UserInDepartmentMapper.toUserInDepartmentDto(chiefEntity);
        }

        return new DepartmentDto(entity.getId(), entity.getName(), chief);
    }

    @Transactional
    public DepartmentDto updateDepartment(UUID id, UpdateDepartmentDto dto) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        UUID oldChiefId = entity.getChiefId();
        entity.setName(dto.name());
        entity.setChiefId(dto.chiefId());
        departmentRepository.save(entity);

        if (oldChiefId != null && !oldChiefId.equals(dto.chiefId())) {
            removeChiefRoleIfNoOtherDepartments(oldChiefId);
        }

        syncChiefRole(dto.chiefId());

        UserInDepartmentDto chief = null;
        if (dto.chiefId() != null) {
            UserEntity chiefEntity = userRepository.findById(dto.chiefId())
                    .orElseThrow(() -> new IllegalArgumentException("Chief not found"));
            chief = UserInDepartmentMapper.toUserInDepartmentDto(chiefEntity);
        }

        return new DepartmentDto(entity.getId(), entity.getName(), chief);
    }

    @Transactional
    public void deleteDepartment(UUID id) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        UUID chiefId = entity.getChiefId();

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

        DepartmentUserEntity entity = new DepartmentUserEntity();
        entity.setId(UUID.randomUUID());
        entity.setUser(user);
        entity.setDepartment(department);
        departmentUserRepository.save(entity);

        UserInDepartmentDto userDto = UserInDepartmentMapper.toUserInDepartmentDto(user);

        return new DepartmentUserDto(entity.getId(), userDto, department.getId());
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
            UserRoleEntity ur = new UserRoleEntity();
            ur.setUserId(chiefId);
            ur.setRoleId(role.getId());
            userRoleRepository.save(ur);
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
