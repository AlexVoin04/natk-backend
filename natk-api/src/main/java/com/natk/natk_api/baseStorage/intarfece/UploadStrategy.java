package com.natk.natk_api.baseStorage.intarfece;

import com.natk.common.messaging.ScanTask;
import com.natk.natk_api.baseStorage.MagicValidationResult;
import com.natk.natk_api.baseStorage.context.StorageContext;

import java.io.InputStream;

public interface UploadStrategy<TFile, TFolder, TFileRepo, TOwner> {

    TOwner getOwner(StorageContext ctx);

    void ensureUniqueNameOrThrow(String name, TFolder folder, TOwner owner);

    MagicValidationResult validateMime(InputStream data, String name);

    String generateIncomingKey(TOwner owner);

    void uploadToMinio(InputStream fullStream, long size, String storageKey, String mimeType);

    ScanTask buildScanTask(TFile file, String key, StorageContext ctx, TOwner owner);

    TFile buildNewFileEntity(String name, String mime, long size,
                             TFolder folder, String storageKey, StorageContext ctx, TOwner owner);

    void persistFile(TFile file);

    TFile reloadAfterSave(TFile file);

    TFileRepo getRepo();
}
