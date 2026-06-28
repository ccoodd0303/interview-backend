package com.project.backend.dto.response;

import java.util.List;

// 시작된 면접 세션 및 질문 목록 정보
public record InterviewStartResponse(
        String interviewId,
        String subject,
        String mode,
        String type,
        List<QuestionResponse> questions
) {
    public InterviewStartResponse(String interviewId, String subject, List<QuestionResponse> questions) {
        this(interviewId, subject, "basic", "GENERAL", questions);
    }
}
