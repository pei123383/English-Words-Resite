package com.englishwords.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度必须在 3 到 64 个字符之间")
    String username,

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度必须在 6 到 128 个字符之间")
    String password,

    @Size(min = 3, max = 64, message = "昵称长度必须在 3 到 64 个字符之间")
    String nickname
) {
}
