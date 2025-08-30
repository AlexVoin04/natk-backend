package com.natk.natk_api.baseStorage.dto.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MoveFolderValidator.class)
public @interface ValidMoveFolder {
    String message() default "Either moveToRoot=true or newParentFolderId must be provided";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}