package com.natk.natk_api.baseStorage.dto.validator;

import com.natk.natk_api.baseStorage.dto.MoveFolderDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MoveFolderValidator implements ConstraintValidator<ValidMoveFolder, MoveFolderDto> {

    @Override
    public boolean isValid(MoveFolderDto dto, ConstraintValidatorContext context) {
        if (dto == null) return true; // @NotNull отдельно если нужно

        if (dto.moveToRoot()) {
            return dto.newParentFolderId() == null;
        }

        return dto.newParentFolderId() != null;
    }
}
