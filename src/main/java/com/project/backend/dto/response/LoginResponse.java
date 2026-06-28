package com.project.backend.dto.response;

// 로그인 성공 시 반환되는 사용자 식별 정보
public record LoginResponse(
        Long userId,
        String nickname
) {}
