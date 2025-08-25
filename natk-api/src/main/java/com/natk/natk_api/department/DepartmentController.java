package com.natk.natk_api.department;

import com.natk.natk_api.department.dto.AddDepartmentUsersDto;
import com.natk.natk_api.department.dto.CreateDepartmentDto;
import com.natk.natk_api.department.dto.DepartmentDto;
import com.natk.natk_api.department.dto.DepartmentUserDto;
import com.natk.natk_api.department.dto.RemoveDepartmentUsersDto;
import com.natk.natk_api.department.dto.UpdateDepartmentDto;
import com.natk.natk_api.department.dto.UserInDepartmentDto;
import com.natk.natk_api.department.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentDto createDepartment(@RequestBody @Valid CreateDepartmentDto dto) {
        return departmentService.createDepartment(dto);
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentDto updateDepartment(@PathVariable UUID departmentId, @RequestBody UpdateDepartmentDto dto) {
        return departmentService.updateDepartment(departmentId, dto);
    }

    @DeleteMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteDepartment(@PathVariable UUID departmentId) {
        departmentService.deleteDepartment(departmentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<DepartmentDto> listDepartments() {
        return departmentService.listDepartments();
    }

    //TODO: фильтрация
    @GetMapping("/{departmentId}/users/not-in")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public List<UserInDepartmentDto> listUsersNotInDepartment(@PathVariable UUID departmentId) {
        return departmentService.listUsersNotInDepartment(departmentId);
    }

    @GetMapping("/{departmentId}/users")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public List<DepartmentUserDto> listDepartmentUsers(@PathVariable UUID departmentId) {
        return departmentService.listDepartmentUsers(departmentId);
    }

    @PostMapping("/{departmentId}/users/{userId}")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public DepartmentUserDto addUser(
            @PathVariable UUID departmentId,
            @PathVariable UUID userId
    ) {
        return departmentService.addUserToDepartment(departmentId, userId);
    }

    @PostMapping("/{departmentId}/users/bulk")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public List<DepartmentUserDto> addUsers(@PathVariable UUID departmentId, @RequestBody @Valid AddDepartmentUsersDto dto) {
        return departmentService.addUsersToDepartment(departmentId, dto);
    }

    @DeleteMapping("/{departmentId}/users/{userId}")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public ResponseEntity<?> removeUser(@PathVariable UUID departmentId, @PathVariable UUID userId
    ) {
        departmentService.removeUserFromDepartment(departmentId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{departmentId}/users")
    @PreAuthorize("hasPermission(#departmentId, 'DEPARTMENT', 'MANAGE')")
    public ResponseEntity<?> removeUsers(
            @PathVariable UUID departmentId,
            @RequestBody @Valid RemoveDepartmentUsersDto dto
    ) {
        departmentService.removeUsersFromDepartment(departmentId, dto);
        return ResponseEntity.ok().build();
    }
}
