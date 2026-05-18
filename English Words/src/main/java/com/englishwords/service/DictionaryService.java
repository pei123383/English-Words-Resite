package com.englishwords.service;

import com.englishwords.config.DictionaryProperties;
import com.englishwords.dto.PageResponse;
import com.englishwords.dto.dictionary.DictionaryEntryResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class DictionaryService {

    private final DictionaryProperties properties;
    private final ResourceLoader resourceLoader;
    private volatile List<DictionaryEntry> entries;

    public DictionaryService(DictionaryProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    public PageResponse<DictionaryEntryResponse> search(String keyword, int page, int size) {
        List<DictionaryEntry> allEntries = entries();
        String normalizedKeyword = normalizeKeyword(keyword);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));

        List<DictionaryEntry> matches = new ArrayList<>();

        for (DictionaryEntry entry : allEntries) {
            if (matches(entry, normalizedKeyword)) {
                matches.add(entry);
            }
        }

        matches.sort((first, second) -> first.searchWord().compareTo(second.searchWord()));

        int start = safePage * safeSize;
        int end = Math.min(start + safeSize, matches.size());
        List<DictionaryEntryResponse> items = start >= matches.size()
            ? List.of()
            : matches.subList(start, end).stream()
                .map(entry -> new DictionaryEntryResponse(entry.word(), entry.translation()))
                .toList();

        long total = matches.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new PageResponse<>(items, total, safePage, safeSize, totalPages);
    }

    private List<DictionaryEntry> entries() {
        List<DictionaryEntry> localEntries = entries;
        if (localEntries == null) {
            synchronized (this) {
                localEntries = entries;
                if (localEntries == null) {
                    localEntries = loadEntries();
                    entries = localEntries;
                }
            }
        }
        return localEntries;
    }

    private List<DictionaryEntry> loadEntries() {
        Resource resource = resourceLoader.getResource(properties.resource());
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );
            CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(reader)
        ) {
            List<DictionaryEntry> loaded = new ArrayList<>();
            for (CSVRecord record : parser) {
                String word = trimToNull(record.get("word"));
                String translation = trimToNull(record.get("translation"));
                if (word == null || translation == null) {
                    continue;
                }
                loaded.add(new DictionaryEntry(word, translation));
            }
            return List.copyOf(loaded);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load dictionary resource", ex);
        }
    }

    private boolean matches(DictionaryEntry entry, String keyword) {
        if (keyword == null) {
            return true;
        }
        return entry.searchWord().contains(keyword) || entry.searchTranslation().contains(keyword);
    }

    private String normalizeKeyword(String keyword) {
        String value = trimToNull(keyword);
        if (value == null) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty() && trimmed.charAt(0) == '\uFEFF') {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record DictionaryEntry(
        String word,
        String translation,
        String searchWord,
        String searchTranslation
    ) {
        DictionaryEntry(String word, String translation) {
            this(
                word,
                translation,
                word.toLowerCase(Locale.ROOT),
                translation.toLowerCase(Locale.ROOT)
            );
        }
    }
}
