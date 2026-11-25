package com.aiparking.security;

import java.util.List;

public record JwtPayload(Long userId, String username, List<String> roles) {
}
