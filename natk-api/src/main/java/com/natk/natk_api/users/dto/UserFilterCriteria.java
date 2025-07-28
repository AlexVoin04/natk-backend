package com.natk.natk_api.users.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.data.domain.Pageable;

public record UserFilterCriteria(String role, String name, String surname,
                                 int page, int size, String sortBy, String direction) {

    public Pageable toPageable() {
        if (sortBy == null || sortBy.isBlank() || sortBy.equalsIgnoreCase("none")) {
            return PageRequest.of(page, size);
        }
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
}
