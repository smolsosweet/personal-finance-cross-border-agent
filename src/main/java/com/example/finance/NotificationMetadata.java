package com.example.finance;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/** Presentation metadata from recorded source timestamps, never the page-render time. */
final class NotificationMetadata {
    private NotificationMetadata() {}

    static Map<String,Object> attach(Map<String,Object> item, String kind, String actionState, LocalDateTime updatedAt) {
        var result = new LinkedHashMap<>(item);
        result.put("notificationKind", kind);
        result.put("actionState", actionState);
        if (updatedAt != null) {
            var instant = updatedAt.atZone(ZoneId.systemDefault()).toInstant();
            result.put("updatedEpoch", instant.toEpochMilli());
            result.put("updatedAt", instant.toString());
        }
        return result;
    }
}
