package com.englishwords.dto.quiz;

import com.englishwords.dto.progress.ProgressResponse;

public record QuizSubmitResponse(
    boolean correct,
    String selectedAnswer,
    String correctAnswer,
    int quality,
    long responseTimeMs,
    ProgressResponse progress
) {
}
