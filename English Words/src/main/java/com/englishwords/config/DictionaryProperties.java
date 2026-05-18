package com.englishwords.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dictionary")
public record DictionaryProperties(
    String resource
) {
    public DictionaryProperties {
        if (resource == null || resource.isBlank()) {
            resource = "classpath:dictionary/en_words.csv";
        }
    }
}
