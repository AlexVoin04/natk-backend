package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileNameResolverService {
    private final UserFileRepository fileRepo;

    public String generateUniqueFileName(String originalName, UserFolderEntity folder, UserEntity user) {
        String baseName;
        String extension;

        int lastDot = originalName.lastIndexOf('.');
        if (lastDot != -1) {
            baseName = originalName.substring(0, lastDot);
            extension = originalName.substring(lastDot);
        } else {
            baseName = originalName;
            extension = "";
        }

        Pattern suffixPattern = Pattern.compile("^(.*)\\((\\d+)\\)$");
        Matcher suffixMatcher = suffixPattern.matcher(baseName);

        String cleanBaseName = baseName;
        if (suffixMatcher.matches()) {
            cleanBaseName = suffixMatcher.group(1);
        }

        Set<String> existingNames = getExistingFileNames(folder, user);
        if (!existingNames.contains(originalName)) {
            return originalName;
        }

        Pattern pattern = Pattern.compile(Pattern.quote(cleanBaseName) + "\\((\\d+)\\)" + Pattern.quote(extension));
        int maxIndex = 0;
        for (String name : existingNames) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                int index = Integer.parseInt(matcher.group(1));
                maxIndex = Math.max(maxIndex, index);
            }
        }

        return cleanBaseName + "(" + (maxIndex + 1) + ")" + extension;
    }

    private Set<String> getExistingFileNames(UserFolderEntity folder, UserEntity user) {
        List<UserFileEntity> files;
        if (folder == null) {
            files = fileRepo.findByCreatedByAndFolderIsNullAndIsDeletedFalse(user);
        } else {
            files = fileRepo.findByFolderAndIsDeletedFalse(folder);
        }
        return files.stream()
                .map(UserFileEntity::getName)
                .collect(Collectors.toSet());
    }

    public void ensureUniqueNameOrThrow(String desiredName, UserFolderEntity folder, UserEntity user) {
        String uniqueName = generateUniqueFileName(desiredName, folder, user);
        if (!uniqueName.equals(desiredName)) {
            throw new IllegalArgumentException("File with the same name already exists. Suggested name: " + uniqueName);
        }
    }
}
