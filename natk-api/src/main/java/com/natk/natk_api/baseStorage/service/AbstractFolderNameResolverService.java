package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.intarfece.FolderNameResolver;
import com.natk.natk_api.exception.DuplicateNameException;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractFolderNameResolverService<TFolder, TOwner>
        implements FolderNameResolver<TFolder, TOwner> {

    protected abstract Set<String> getExistingFolderNames(TFolder parentFolder, TOwner owner, UUID excludeFolderId);

    @Override
    public void ensureUniqueNameOrThrow(String desiredName, TFolder parentFolder, TOwner owner) {
        ensureUniqueNameOrThrow(desiredName, parentFolder, owner, null);
    }

    @Override
    public void ensureUniqueNameOrThrow(String desiredName, TFolder parentFolder, TOwner owner, UUID excludeFolderId) {
        Set<String> existingNames = getExistingFolderNames(parentFolder, owner, excludeFolderId);
        String uniqueName = generateUniqueFolderName(desiredName, existingNames);
        if (!uniqueName.equals(desiredName)) {
            throw new DuplicateNameException(
                    "Folder with the same name already exists.",
                    uniqueName
            );
        }
    }

    @Override
    public String ensureUniqueName(String desiredName, TFolder parentFolder, TOwner owner, UUID excludeFolderId) {
        Set<String> existingNames = getExistingFolderNames(parentFolder, owner, excludeFolderId);
        return generateUniqueFolderName(desiredName, existingNames);
    }

    private String generateUniqueFolderName(String originalName, Set<String> existingNames) {
        String cleanBaseName = originalName;

        // Проверяем, есть ли уже суффикс (N)
        Pattern suffixPattern = Pattern.compile("^(.*)\\((\\d+)\\)$");
        Matcher suffixMatcher = suffixPattern.matcher(originalName);
        if (suffixMatcher.matches()) {
            cleanBaseName = suffixMatcher.group(1).trim();
        }

        // Если такого имени нет → возвращаем оригинал
        if (!existingNames.contains(originalName)) {
            return originalName;
        }

        // Ищем максимальный индекс
        Pattern pattern = Pattern.compile(Pattern.quote(cleanBaseName) + "\\((\\d+)\\)");
        int maxIndex = 0;
        for (String name : existingNames) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                int index = Integer.parseInt(matcher.group(1));
                maxIndex = Math.max(maxIndex, index);
            }
        }

        return cleanBaseName + "(" + (maxIndex + 1) + ")";
    }
}
