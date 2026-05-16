package com.englishwords.dto.progress;

import java.time.LocalDateTime;

public record ProgressResponse(
    Long wordId,
    String word,
    String translation,
    int reviewCount,
    int correctCount,
    int wrongCount,
    double easeFactor,
    int intervalDays,
    int repetition,
    int masteryLevel,
    LocalDateTime nextReviewAt,
    LocalDateTime lastReviewedAt
) {
}
