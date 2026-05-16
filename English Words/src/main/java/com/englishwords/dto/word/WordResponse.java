package com.englishwords.dto.word;

import java.time.LocalDateTime;

public record WordResponse(
    Long id,
    Long bookId,
    String bookName,
    String word,
    String translation,
    String phonetic,
    String example,
    String tags,
    int reviewCount,
    int correctCount,
    int wrongCount,
    double easeFactor,
    int intervalDays,
    int repetition,
    int masteryLevel,
    LocalDateTime nextReviewAt,
    LocalDateTime lastReviewedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
