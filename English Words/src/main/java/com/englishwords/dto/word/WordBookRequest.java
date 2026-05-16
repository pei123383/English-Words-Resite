package com.englishwords.dto.word;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordBookRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description
) {
}
