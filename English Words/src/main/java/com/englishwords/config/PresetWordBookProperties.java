package com.englishwords.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.preset-word-book")
public record PresetWordBookProperties(
    boolean enabled,
    String key,
    String name,
    String description,
    String resource
) {
}
