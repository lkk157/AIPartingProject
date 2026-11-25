package com.aiparking.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserVO(
        Long id,
        String username,
        String nickname,
        String phone,
        Integer status,
        List<String> roles,
        LocalDateTime createdAt
) {
}
