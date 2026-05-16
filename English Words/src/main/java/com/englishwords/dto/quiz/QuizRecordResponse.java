package com.englishwords.dto.quiz;

import com.englishwords.enums.QuizMode;
import java.time.LocalDateTime;

public record QuizRecordResponse(
    Long id,
    Long wordId,
    String word,
    String translation,
    QuizMode mode,
    int quality,
    boolean remembered,
    LocalDateTime createdAt
) {
}
