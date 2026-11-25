package com.aiparking.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void should_match_when_password_correct() {
        String hash = encoder.encode("secret123");
        assertThat(encoder.matches("secret123", hash)).isTrue();
    }

    @Test
    void should_not_match_when_password_wrong() {
        String hash = encoder.encode("secret123");
        assertThat(encoder.matches("secret124", hash)).isFalse();
    }

    @Test
    void should_produce_different_hashes_for_same_password() {
        String first = encoder.encode("secret123");
        String second = encoder.encode("secret123");

        assertThat(first).isNotEqualTo(second);
        assertThat(encoder.matches("secret123", first)).isTrue();
        assertThat(encoder.matches("secret123", second)).isTrue();
    }

    @Test
    void should_not_store_plain_text() {
        String hash = encoder.encode("secret123");
        assertThat(hash).doesNotContain("secret123");
        assertThat(hash).startsWith("$2");
    }
}
