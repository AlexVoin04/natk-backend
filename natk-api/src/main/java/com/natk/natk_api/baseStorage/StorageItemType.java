package com.natk.natk_api.baseStorage;

public enum StorageItemType {
    FILE, FOLDER;

    public static StorageItemType from(String v) {
        try {
            return valueOf(v.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown type: " + v);
        }
    }
}
