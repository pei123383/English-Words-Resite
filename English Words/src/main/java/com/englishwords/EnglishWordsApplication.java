package com.englishwords;

import com.englishwords.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class EnglishWordsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishWordsApplication.class, args);
    }
}
