package com.project.backend.dto.response;

import java.util.List;

// 시작된 면접 세션의 식별 정보와 함께 출제된 문제 목록을 반환하는 DTO
public record InterviewStartResponse(
        String interviewId,
        String subject,
        String mode,
        String type,
        List<QuestionResponse> questions
) {
    public InterviewStartResponse(String interviewId, String subject, List<QuestionResponse> questions) {
        this(interviewId, subject, "general", "GENERAL", questions);
    }
}
