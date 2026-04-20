package com.natk.natk_api.departmentStorage.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.natk.natk_api.baseStorage.NativeQueryUtils.getFileStatus;
import static com.natk.natk_api.baseStorage.NativeQueryUtils.getInstant;
import static com.natk.natk_api.baseStorage.NativeQueryUtils.getLong;
import static com.natk.natk_api.baseStorage.NativeQueryUtils.getString;
import static com.natk.natk_api.baseStorage.NativeQueryUtils.getUuid;

@Repository
@RequiredArgsConstructor
public class DepartmentStorageSearchRepositoryImpl implements DepartmentStorageSearchRepository {

    private final EntityManager em;

    private static final String SEARCH_ALL_SQL = """
        select * from (
            select
                f.id as "id",
                f.name as "name",
                'folder' as "type",
                f.created_at as "createdAt",
                f.updated_at as "updatedAt",
                f.created_by as "createdBy",
                null::file_status as "fileAntivirusStatus",
                null::bigint as "size",
                0 as "typeOrder"
            from department_folders f
            left join department_folder_access a
                   on a.folder_id = f.id
                  and a.user_id = :userId
            where f.department_id = :deptId
              and f.is_deleted = false
              and (f.is_public = true or a.id is not null)
              and lower(f.name) like lower(concat('%', :query, '%'))

            union all

            select
                u.id as "id",
                u.name as "name",
                coalesce(u.file_type, 'file') as "type",
                u.created_at as "createdAt",
                null::timestamp as "updatedAt",
                u.created_by as "createdBy",
                u.status as "fileAntivirusStatus",
                u.file_size as "size",
                1 as "typeOrder"
            from department_files u
            join department_folders f on u.folder_id = f.id
            left join department_folder_access a
                   on a.folder_id = f.id
                  and a.user_id = :userId
            where u.department_id = :deptId
              and u.is_deleted = false
              and (f.is_public = true or a.id is not null)
              and lower(u.name) like lower(concat('%', :query, '%'))
        ) x
        order by x."typeOrder", lower(x."name")
        limit 200
        """;

    private static final String SEARCH_FOLDERS_SQL = """
        select
            f.id as "id",
            f.name as "name",
            'folder' as "type",
            f.created_at as "createdAt",
            f.updated_at as "updatedAt",
            f.created_by as "createdBy",
            null::file_status as "fileAntivirusStatus",
            null::bigint as "size"
        from department_folders f
        left join department_folder_access a
               on a.folder_id = f.id
              and a.user_id = :userId
        where f.department_id = :deptId
          and f.is_deleted = false
          and (f.is_public = true or a.id is not null)
          and lower(f.name) like lower(concat('%', :query, '%'))
        order by lower(f.name)
        limit 200
        """;

    private static final String SEARCH_FILES_SQL = """
        select
            u.id as "id",
            u.name as "name",
            coalesce(u.file_type, 'file') as "type",
            u.created_at as "createdAt",
            null::timestamp as "updatedAt",
            u.created_by as "createdBy",
            u.status as "fileAntivirusStatus",
            u.file_size as "size"
        from department_files u
        join department_folders f on u.folder_id = f.id
        left join department_folder_access a
               on a.folder_id = f.id
              and a.user_id = :userId
        where u.department_id = :deptId
          and u.is_deleted = false
          and (f.is_public = true or a.id is not null)
          and lower(u.name) like lower(concat('%', :query, '%'))
        order by lower(u.name)
        limit 200
        """;


    @Override
    @Transactional(readOnly = true)
    public List<DepartmentStorageSearchRow> searchAll(UUID deptId, UUID userId, String query) {
        return execute(SEARCH_ALL_SQL, deptId, userId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentStorageSearchRow> searchFolders(UUID deptId, UUID userId, String query) {
        return execute(SEARCH_FOLDERS_SQL, deptId, userId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentStorageSearchRow> searchFiles(UUID deptId, UUID userId, String query) {
        return execute(SEARCH_FILES_SQL, deptId, userId, query);
    }

    @SuppressWarnings("unchecked")
    private List<DepartmentStorageSearchRow> execute(String sql, UUID deptId, UUID userId, String query) {
        NativeQuery<?> q = em.createNativeQuery(sql).unwrap(NativeQuery.class);
        q.setParameter("deptId", deptId);
        q.setParameter("userId", userId);
        q.setParameter("query", query);

        q.setTupleTransformer(this::mapRow);

        return (List<DepartmentStorageSearchRow>) q.getResultList();
    }

    private DepartmentStorageSearchRow mapRow(Object[] t, String[] a) {
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < a.length; i++) {
            indexMap.put(a[i].toLowerCase(), i);
        }

        return new DepartmentStorageSearchRow(
                getUuid(t, indexMap, "id"),
                getString(t, indexMap, "name"),
                getString(t, indexMap, "type"),
                getInstant(t, indexMap, "createdAt"),
                getInstant(t, indexMap, "updatedAt"),
                getString(t, indexMap, "createdBy"),
                getFileStatus(t, indexMap, "fileAntivirusStatus"),
                getLong(t, indexMap, "size")
        );
    }
}
