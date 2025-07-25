package com.natk.natk_api.users.service;

import com.natk.natk_api.users.dto.UserFilterCriteria;
import com.natk.natk_api.users.dto.UserUpdateDto;
import com.natk.natk_api.users.model.RoleEntity;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.repository.RoleRepository;
import com.natk.natk_api.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserService currentUserService;
    private final AccessControlService accessControlService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       CurrentUserService currentUserService,
                       AccessControlService accessControlService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.currentUserService = currentUserService;
        this.accessControlService = accessControlService;
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<UserEntity> getUsersFiltered(UserFilterCriteria criteria) {
        Pageable pageable = criteria.toPageable();
        return userRepository.findByFilters(
                criteria.role(), criteria.name(), criteria.surname(), pageable
        );
    }

    public UserEntity getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found. ID:" + id));
    }

    public UserEntity updateUser(UUID id, UserUpdateDto dto) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        UserEntity userToUpdate = getUserById(id);

        if (!accessControlService.canUpdateUser(currentUser, userToUpdate)) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        applyUpdate(userToUpdate, dto, currentUser);
        return userRepository.save(userToUpdate);
    }

    private void applyUpdate(UserEntity user, UserUpdateDto dto, UserEntity currentUser) {
        if (dto.name() != null) user.setName(dto.name());
        if (dto.surname() != null) user.setSurname(dto.surname());
        if (dto.patronymic() != null) user.setPatronymic(dto.patronymic());
        if (dto.phoneNumber() != null) user.setPhoneNumber(dto.phoneNumber());

        if (accessControlService.isAdmin(currentUser) && dto.roles() != null) {
            List<RoleEntity> newRoles = dto.roles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + roleName)))
                    .toList();
            user.getRoles().clear();
            user.getRoles().addAll(newRoles);
        }
    }

    public void deleteUserById(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found. ID:" + id);
        }
        userRepository.deleteById(id);
    }
}
