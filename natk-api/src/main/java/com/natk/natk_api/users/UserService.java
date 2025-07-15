package com.natk.natk_api.users;

import com.natk.natk_api.service.JwtService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(JwtService jwtService, UserRepository userRepository) {

        this.userRepository = userRepository;
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
            //TODO: надо пофиксить пагинацию
            pageable = PageRequest.of(page, size, sort);
        }

        return userRepository.findByFilters(role, name, surname, pageable);
    }

    public UserEntity getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
