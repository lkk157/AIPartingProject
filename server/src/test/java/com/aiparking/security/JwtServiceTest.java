package com.aiparking.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-with-32-bytes!!");
        properties.setExpireMinutes(60);
        properties.setIssuer("parking-test");
    }

    @Test
    void should_generate_and_parse_claims_when_token_valid() {
        JwtService jwtService = new JwtService(properties);

        String token = jwtService.generate(7L, "alice", List.of("USER", "SYS_ADMIN"));
        JwtPayload payload = jwtService.parse(token);

        assertThat(payload.userId()).isEqualTo(7L);
        assertThat(payload.username()).isEqualTo("alice");
        assertThat(payload.roles()).containsExactly("USER", "SYS_ADMIN");
    }

    @Test
    void should_reject_token_when_expired() {
        properties.setExpireMinutes(-1);
        JwtService jwtService = new JwtService(properties);

        String token = jwtService.generate(1L, "bob", List.of("USER"));

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void should_reject_token_when_tampered() {
        JwtService jwtService = new JwtService(properties);
        String token = jwtService.generate(1L, "bob", List.of("USER"));

        assertThatThrownBy(() -> jwtService.parse(token + "x"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void should_reject_token_when_signed_by_other_secret() {
        JwtService issuer = new JwtService(properties);

        JwtProperties other = new JwtProperties();
        other.setSecret("another-secret-key-with-32-bytes!!!!");
        other.setExpireMinutes(60);
        other.setIssuer("parking-test");
        JwtService verifier = new JwtService(other);

        String token = issuer.generate(1L, "bob", List.of("USER"));

        assertThatThrownBy(() -> verifier.parse(token))
                .isInstanceOf(JwtException.class);
    }
}
