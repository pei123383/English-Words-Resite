package com.englishwords.dto.word;

import java.time.LocalDateTime;

public record WordBookResponse(
    Long id,
    String name,
    String description,
    long wordCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
