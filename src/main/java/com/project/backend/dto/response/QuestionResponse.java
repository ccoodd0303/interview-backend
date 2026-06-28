package com.project.backend.dto.response;

// 면접 진행 화면용 질문 정보 (ID, 과목, 본문)
public record QuestionResponse(
        Long questionId,
        String subject,
        String questionText
) {}
