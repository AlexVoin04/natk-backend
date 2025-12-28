package com.natk.natk_api.baseStorage.enums;

public enum BucketName {

    DEPARTMENTS_FILES("department-files"),
    INCOMING("incoming"),
    USER_FILES("user-files");

    private final String value;

    BucketName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}