package com.natk.natk_api.exception;

public class FileOrFolderNotFoundOrNoAccessException extends RuntimeException {
    public FileOrFolderNotFoundOrNoAccessException() {
        super("Not found or access denied");
    }
}