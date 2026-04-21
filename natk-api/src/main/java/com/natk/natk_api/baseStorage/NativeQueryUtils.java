package com.natk.natk_api.baseStorage;

import com.natk.natk_api.baseStorage.enums.FileStatus;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NativeQueryUtils {
    public static Map<String, Integer> buildIndexMap(String[] aliases) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < aliases.length; i++) {
            map.put(aliases[i].toLowerCase(), i);
        }
        return map;
    }

    public static Object getValue(Object[] tuple, Map<String, Integer> indexMap, String name) {
        Integer index = indexMap.get(name.toLowerCase());
        if (index == null) return null;
        return tuple[index];
    }

    public static UUID getUuid(Object[] tuple, Map<String, Integer> indexMap, String name) {
        Object v = getValue(tuple, indexMap, name);
        return v == null ? null : UUID.fromString(v.toString());
    }

    public static String getString(Object[] tuple, Map<String, Integer> indexMap, String name) {
        Object v = getValue(tuple, indexMap, name);
        return v == null ? null : v.toString();
    }

    public static Long getLong(Object[] tuple, Map<String, Integer> indexMap, String name) {
        Object v = getValue(tuple, indexMap, name);
        if (v instanceof Number n) return n.longValue();
        return v == null ? null : Long.parseLong(v.toString());
    }

    public static Instant getInstant(Object[] tuple, Map<String, Integer> indexMap, String name) {
        Object v = getValue(tuple, indexMap, name);
        if (v instanceof Timestamp ts) return ts.toInstant();
        if (v instanceof Instant inst) return inst;
        return null;
    }

    public static FileStatus getFileStatus(Object[] tuple, Map<String, Integer> indexMap, String name) {
        Object v = getValue(tuple, indexMap, name);
        return v == null ? null : FileStatus.valueOf(v.toString());
    }
}
