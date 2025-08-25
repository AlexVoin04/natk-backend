package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.intarfece.FolderNameResolver;
import com.natk.natk_api.exception.DuplicateNameException;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractFolderNameResolverService<TFolder, TOwner>
        implements FolderNameResolver<TFolder, TOwner> {

    protected abstract Set<String> getExistingFolderNames(TFolder parentFolder, TOwner owner);

    public String generateUniqueFolderName(String originalName, TFolder parentFolder, TOwner owner) {
        String cleanBaseName = originalName;

        // Проверяем, есть ли уже суффикс (N)
        Pattern suffixPattern = Pattern.compile("^(.*)\\((\\d+)\\)$");
        Matcher suffixMatcher = suffixPattern.matcher(originalName);
        if (suffixMatcher.matches()) {
            cleanBaseName = suffixMatcher.group(1).trim();
        }

        Set<String> existingNames = getExistingFolderNames(parentFolder, owner);

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

    @Override
    public void ensureUniqueNameOrThrow(String desiredName, TFolder parentFolder, TOwner owner) {
        String uniqueName = generateUniqueFolderName(desiredName, parentFolder, owner);
        if (!uniqueName.equals(desiredName)) {
            throw new DuplicateNameException(
                    "Folder with the same name already exists.",
                    uniqueName
            );
        }
    }
}
