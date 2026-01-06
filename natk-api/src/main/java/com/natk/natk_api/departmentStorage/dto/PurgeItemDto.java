package com.natk.natk_api.departmentStorage.dto;

import java.util.UUID;

/**
 * Пакетное удаление: в теле принимается список id и тип ("FILE" или "FOLDER")
 * Пример тела:
 * [{ "id": "uuid", "type": "FILE" }, { "id":"uuid2", "type":"FOLDER" }]
 */
public record PurgeItemDto(UUID id, String type) {}