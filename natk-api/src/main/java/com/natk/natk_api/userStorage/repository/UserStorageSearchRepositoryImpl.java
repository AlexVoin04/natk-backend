package com.natk.natk_api.userStorage.repository;

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
public class UserStorageSearchRepositoryImpl implements UserStorageSearchRepository {

    private static final String SEARCH_ALL_SQL = """
        select * from (
            select
                f.id as "id",
                f.name as "name",
                'folder' as "type",
                f.created_at as "createdAt",
                f.updated_at as "updatedAt",
                null::file_status as "fileAntivirusStatus",
                null::bigint as "size",
                0 as "typeOrder"
            from user_folders f
            where f.user_id = :userId
              and f.is_deleted = false
              and lower(f.name) like lower(concat('%', :query, '%'))

            union all

            select
                u.id as "id",
                u.name as "name",
                coalesce(u.file_type, 'file') as "type",
                u.created_at as "createdAt",
                null::timestamp as "updatedAt",
                u.status as "fileAntivirusStatus",
                u.file_size as "size",
                1 as "typeOrder"
            from user_files u
            where u.created_by = :userId
              and u.is_deleted = false
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
            null::file_status as "fileAntivirusStatus",
            null::bigint as "size"
        from user_folders f
        where f.user_id = :userId
          and f.is_deleted = false
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
            u.status as "fileAntivirusStatus",
            u.file_size as "size"
        from user_files u
        where u.created_by = :userId
          and u.is_deleted = false
          and lower(u.name) like lower(concat('%', :query, '%'))
        order by lower(u.name)
        limit 200
        """;

    private final EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public List<UserStorageSearchRow> searchAll(UUID userId, String query) {
        return execute(SEARCH_ALL_SQL, userId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStorageSearchRow> searchFolders(UUID userId, String query) {
        return execute(SEARCH_FOLDERS_SQL, userId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStorageSearchRow> searchFiles(UUID userId, String query) {
        return execute(SEARCH_FILES_SQL, userId, query);
    }

    @SuppressWarnings("unchecked")
    private List<UserStorageSearchRow> execute(String sql, UUID userId, String query) {
        NativeQuery<?> q = em.createNativeQuery(sql).unwrap(NativeQuery.class);
        q.setParameter("userId", userId);
        q.setParameter("query", query);

        q.setTupleTransformer(this::mapRow);

        return (List<UserStorageSearchRow>) q.getResultList();
    }

    private UserStorageSearchRow mapRow(Object[] tuple, String[] aliases) {
        // Создаем карту соответствия: имя колонки -> индекс в массиве tuple
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < aliases.length; i++) {
            indexMap.put(aliases[i].toLowerCase(), i);
        }

        return new UserStorageSearchRow(
                getUuid(tuple, indexMap, "id"),
                getString(tuple, indexMap, "name"),
                getString(tuple, indexMap, "type"),
                getInstant(tuple, indexMap, "createdAt"),
                getInstant(tuple, indexMap, "updatedAt"),
                getFileStatus(tuple, indexMap, "fileAntivirusStatus"),
                getLong(tuple, indexMap, "size")
        );
    }
}
