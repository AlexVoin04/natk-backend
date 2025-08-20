package com.natk.natk_api.department;

import com.natk.natk_api.department.dto.AddDepartmentUserDto;
import com.natk.natk_api.department.dto.AddDepartmentUsersDto;
import com.natk.natk_api.department.dto.CreateDepartmentDto;
import com.natk.natk_api.department.dto.DepartmentDto;
import com.natk.natk_api.department.dto.DepartmentUserDto;
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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentDto updateDepartment(@PathVariable UUID id, @RequestBody UpdateDepartmentDto dto) {
        return departmentService.updateDepartment(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteDepartment(@PathVariable UUID id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<DepartmentDto> listDepartments() {
        return departmentService.listDepartments();
    }

    //TODO: фильтрация
    @GetMapping("/{id}/users/not-in")
    @PreAuthorize("hasRole('ADMIN') or @departmentSecurity.isChief(authentication, #id)")
    public List<UserInDepartmentDto> listUsersNotInDepartment(@PathVariable UUID id) {
        return departmentService.listUsersNotInDepartment(id);
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasRole('ADMIN') or @departmentSecurity.isChief(authentication, #id)")
    public List<DepartmentUserDto> listDepartmentUsers(@PathVariable UUID id) {
        return departmentService.listDepartmentUsers(id);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or @departmentSecurity.isChief(authentication, #dto.departmentId())")
    public DepartmentUserDto addUser(@RequestBody @Valid AddDepartmentUserDto dto) {
        return departmentService.addUserToDepartment(dto);
    }

    @PostMapping("/users/bulk")
    @PreAuthorize("hasRole('ADMIN') or @departmentSecurity.isChief(authentication, #dto.departmentId())")
    public List<DepartmentUserDto> addUsers(@RequestBody @Valid AddDepartmentUsersDto dto) {
        return departmentService.addUsersToDepartment(dto);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN') or @departmentSecurity.isChief(authentication, @departmentService.getDepartmentIdByDepartmentUserId(#id))")
    public ResponseEntity<?> removeUser(@PathVariable UUID id) {
        departmentService.removeUserFromDepartment(id);
        return ResponseEntity.ok().build();
    }
}
