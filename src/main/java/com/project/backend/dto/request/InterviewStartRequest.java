package com.project.backend.dto.request;

// 면접 시작 요청 데이터
public record InterviewStartRequest(
        Long userId,
        String subject,
        String mode,
        String type,
        Integer questionCount
) {}
