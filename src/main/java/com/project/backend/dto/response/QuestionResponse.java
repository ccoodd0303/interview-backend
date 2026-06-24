package com.project.backend.dto.response;

// 면접 진행 시 화면에 보여줄 개별 질문의 ID, 과목, 본문 텍스트를 반환하는 DTO
public record QuestionResponse(
        Long questionId,
        String subject,
        String questionText
) {}
