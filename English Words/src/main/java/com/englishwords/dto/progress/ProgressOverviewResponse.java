package com.englishwords.dto.progress;

public record ProgressOverviewResponse(
    long totalWords,
    long dueWords,
    long masteredWords,
    long totalReviews
) {
}
