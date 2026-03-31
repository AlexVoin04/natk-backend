package com.natk.natk_api.userStorage.repository;

import java.util.List;
import java.util.UUID;

public interface UserStorageSearchRepository{
    List<UserStorageSearchRow> searchAll(UUID userId, String query);
    List<UserStorageSearchRow> searchFolders(UUID userId, String query);
    List<UserStorageSearchRow> searchFiles(UUID userId, String query);
}