package com.englishwords.dto.word;

import java.util.List;

public record WordImportResponse(
    int totalRows,
    int createdCount,
    int updatedCount,
    int skippedCount,
    List<String> errors
) {
}
