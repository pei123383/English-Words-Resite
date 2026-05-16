package com.englishwords.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishwords.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void generatesAndParsesToken() {
        JwtService jwtService = new JwtService(new JwtProperties("test-secret-for-english-words-backend", 3600000));
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");

        String token = jwtService.generateToken(user);
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get("userId", Number.class).longValue()).isEqualTo(7L);
        assertThat(jwtService.isValid(token)).isTrue();
    }
}
