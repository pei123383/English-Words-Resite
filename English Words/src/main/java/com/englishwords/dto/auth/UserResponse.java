package com.englishwords.dto.auth;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String nickname,
    LocalDateTime createdAt
) {
}
