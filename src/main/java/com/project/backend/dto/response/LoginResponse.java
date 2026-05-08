package com.project.backend.dto.response;

public record LoginResponse(
        Long userId,
        String nickname
) {}
