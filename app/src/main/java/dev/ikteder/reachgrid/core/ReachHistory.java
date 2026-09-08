package dev.ikteder.reachgrid.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public final class ReachHistory {
    public static final int SCHEMA_VERSION = 1;
    public static final int DEFAULT_LIMIT = 20;

    public static final class Entry {
        public final long savedAtEpochMilliseconds;
        public final String reportJson;

        Entry(long savedAtEpochMilliseconds, String reportJson) {
            this.savedAtEpochMilliseconds = savedAtEpochMilliseconds;
            this.reportJson = reportJson;
        }
    }

    private ReachHistory() {}

    public static String append(String stored, long savedAtEpochMilliseconds, ReachReport report) {
        return append(stored, savedAtEpochMilliseconds, report, DEFAULT_LIMIT);
    }

    public static String append(
            String stored, long savedAtEpochMilliseconds, ReachReport report, int limit) {
        if (savedAtEpochMilliseconds < 0L) {
            throw new IllegalArgumentException("save timestamp must be non-negative");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("history limit must be positive");
        }
        List<Entry> entries = new ArrayList<>(entries(stored));
        entries.add(new Entry(savedAtEpochMilliseconds, report.toJson()));
        int start = Math.max(0, entries.size() - limit);
        StringBuilder encoded = new StringBuilder();
        for (int index = start; index < entries.size(); index += 1) {
            if (encoded.length() > 0) encoded.append('\n');
            Entry entry = entries.get(index);
            byte[] reportBytes = entry.reportJson.getBytes(StandardCharsets.UTF_8);
            encoded.append(entry.savedAtEpochMilliseconds).append(':')
                    .append(sha256(reportBytes)).append(':')
                    .append(Base64.getEncoder().encodeToString(reportBytes));
        }
        return encoded.toString();
    }

    public static List<Entry> entries(String stored) {
        if (stored == null || stored.trim().isEmpty()) return Collections.emptyList();
        List<Entry> entries = new ArrayList<>();
        String[] lines = stored.split("\\r?\\n");
        for (String line : lines) {
            int firstSeparator = line.indexOf(':');
            int secondSeparator = line.indexOf(':', firstSeparator + 1);
            if (firstSeparator <= 0 || secondSeparator <= firstSeparator + 1
                    || secondSeparator == line.length() - 1) continue;
            try {
                long timestamp = Long.parseLong(line.substring(0, firstSeparator));
                if (timestamp < 0L) continue;
                String expectedDigest = line.substring(firstSeparator + 1, secondSeparator);
                byte[] reportBytes = Base64.getDecoder().decode(line.substring(secondSeparator + 1));
                if (!expectedDigest.equals(sha256(reportBytes))) continue;
                String json = new String(reportBytes, StandardCharsets.UTF_8);
                if (!json.startsWith("{\"schemaVersion\":") || !json.endsWith("}")) continue;
                entries.add(new Entry(timestamp, json));
            } catch (IllegalArgumentException ignored) {
                // A damaged private-preference entry is skipped without hiding later valid evidence.
            }
        }
        return Collections.unmodifiableList(entries);
    }

    private static String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static int count(String stored) {
        return entries(stored).size();
    }

    public static String toJson(String stored) {
        List<Entry> entries = entries(stored);
        StringBuilder json = new StringBuilder();
        json.append("{\"schemaVersion\":").append(SCHEMA_VERSION)
                .append(",\"sessionCount\":").append(entries.size())
                .append(",\"sessions\":[");
        for (int index = 0; index < entries.size(); index += 1) {
            if (index > 0) json.append(',');
            Entry entry = entries.get(index);
            json.append("{\"savedAtEpochMilliseconds\":")
                    .append(entry.savedAtEpochMilliseconds)
                    .append(",\"report\":").append(entry.reportJson).append('}');
        }
        return json.append("]}").toString();
    }
}
