package com.project.backend.dto.request;

// 면접 시작 요청 시 사용자 ID, 과목명, 면접 모드/타입 정보를 전달하는 DTO
public record InterviewStartRequest(
        Long userId,
        String subject,
        String mode,
        String type,
        Integer questionCount
) {}
