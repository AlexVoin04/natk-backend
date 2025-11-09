package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.intarfece.FileNameResolver;
import com.natk.natk_api.exception.DuplicateNameException;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractFileNameResolverService<TFolder, TOwner>
        implements FileNameResolver<TFolder, TOwner> {

    protected abstract Set<String> getExistingFileNames(TFolder parentFolder, TOwner owner, UUID excludeFileId);

    @Override
    public void ensureUniqueNameOrThrow(String desiredName, TFolder parentFolder, TOwner owner) {
        ensureUniqueNameOrThrow(desiredName, parentFolder, owner, null);
    }

    @Override
    public void ensureUniqueNameOrThrow(String desiredName, TFolder parentFolder, TOwner owner, UUID excludeFileId) {
        Set<String> existingNames = getExistingFileNames(parentFolder, owner, excludeFileId);
        String uniqueName = generateUniqueFileName(desiredName, existingNames);
        if (!uniqueName.equals(desiredName)) {
            throw new DuplicateNameException("File with the same name already exists.", uniqueName);
        }
    }

    @Override
    public String ensureUniqueName(String desiredName, TFolder parentFolder, TOwner owner, UUID excludeFileId) {
        Set<String> existingNames = getExistingFileNames(parentFolder, owner, excludeFileId);
        return generateUniqueFileName(desiredName, existingNames);
    }

    /**
     * Логика генерации уникального имени.
     * Сохраняет расширение (например ".pdf"), учитывает существующие суффиксы "(N)".
     */
    private String generateUniqueFileName(String originalName, Set<String> existingNames) {
        if (originalName == null || originalName.isBlank()) return originalName;

        String base;
        String extension = "";
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot > 0) { // >0 чтобы игнорировать файлы типа ".bashrc"
            base = originalName.substring(0, lastDot);
            extension = originalName.substring(lastDot);
        } else {
            base = originalName;
        }

        Pattern suffixPattern = Pattern.compile("^(.*)\\((\\d+)\\)\\s*$");
        Matcher suffixMatcher = suffixPattern.matcher(base);
        String cleanBase = suffixMatcher.matches() ? suffixMatcher.group(1).trim() : base;

        if (!existingNames.contains(originalName)) {
            return originalName;
        }

        Pattern pattern = Pattern.compile(Pattern.quote(cleanBase) + "\\((\\d+)\\)" + Pattern.quote(extension) + "$");

        int maxIndex = 0;
        for (String name : existingNames) {
            Matcher m = pattern.matcher(name);
            if (m.find()) {
                try {
                    int idx = Integer.parseInt(m.group(1));
                    maxIndex = Math.max(maxIndex, idx);
                } catch (NumberFormatException ignored) { }
            }
        }

        return cleanBase + "(" + (maxIndex + 1) + ")" + extension;
    }
}
