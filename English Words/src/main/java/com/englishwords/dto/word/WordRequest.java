package com.englishwords.dto.word;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WordRequest(
    @NotNull Long bookId,
    @NotBlank @Size(max = 160) String word,
    @NotBlank @Size(max = 1000) String translation,
    @Size(max = 160) String phonetic,
    @Size(max = 1000) String example,
    @Size(max = 500) String tags
) {
}
