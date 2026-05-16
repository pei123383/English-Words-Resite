package com.englishwords.dto.quiz;

import com.englishwords.enums.QuizMode;
import java.util.List;

public record QuizQuestionResponse(
    Long wordId,
    Long bookId,
    QuizMode mode,
    String prompt,
    String answer,
    List<String> options,
    String word,
    String translation,
    String phonetic,
    String example,
    String tags,
    int masteryLevel
) {
}
