package com.natk.natk_api.departmentStorage.repository;

import java.util.List;
import java.util.UUID;

public interface DepartmentStorageSearchRepository {
    List<DepartmentStorageSearchRow> searchAll(UUID deptId, UUID userId, String query);
    List<DepartmentStorageSearchRow> searchFolders(UUID deptId, UUID userId, String query);
    List<DepartmentStorageSearchRow> searchFiles(UUID deptId, UUID userId, String query);
}