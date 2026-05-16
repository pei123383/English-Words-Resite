package com.englishwords.dto.quiz;

import com.englishwords.enums.QuizMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record QuizSubmitRequest(
    @NotNull Long wordId,
    @NotNull QuizMode mode,
    @NotBlank String selectedAnswer,
    @PositiveOrZero long responseTimeMs
) {
}
