package com.natk.natk_api.users;

import com.natk.natk_api.service.JwtService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(JwtService jwtService, UserRepository userRepository, RoleRepository roleRepository) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new IllegalStateException("Unauthorized");
        }
        return ((CustomUserDetails) auth.getPrincipal()).getUser();
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<UserEntity> getUsersFiltered(String role, String name, String surname,
                                             int page, int size, String sortBy, String direction) {

        Pageable pageable;
        if (sortBy == null || sortBy.isBlank() || sortBy.equalsIgnoreCase("none")) {
            pageable = PageRequest.of(page, size); // без сортировки
        } else {
            Sort sort = direction.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            pageable = PageRequest.of(page, size, sort);
        }

        return userRepository.findByFilters(role, name, surname, pageable);
    }

    public UserEntity getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public UserEntity updateUser(UUID id, UserUpdateDto dto) {
        UserEntity currentUser = getCurrentUser();
        UserEntity userToUpdate = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("ADMIN"));

        boolean isSelf = currentUser.getId().equals(userToUpdate.getId());

        if (!isSelf && !isAdmin) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        if (dto.name() != null) userToUpdate.setName(dto.name());
        if (dto.surname() != null) userToUpdate.setSurname(dto.surname());
        if (dto.patronymic() != null) userToUpdate.setPatronymic(dto.patronymic());
        if (dto.phoneNumber() != null) userToUpdate.setPhoneNumber(dto.phoneNumber());

        // админ может менять роли
        if (isAdmin && dto.roles() != null) {
            List<RoleEntity> newRoles = dto.roles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + roleName)))
                    .toList();
            userToUpdate.getRoles().clear();
            userToUpdate.getRoles().addAll(newRoles);
        }

        return userRepository.save(userToUpdate);
    }
}
