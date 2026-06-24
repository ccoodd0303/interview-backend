package com.project.backend.dto.request;

// 꼬리 질문 답변 제출 요청을 전달하는 DTO
public record FollowUpAnswerRequest(
        Long answerId,
        Long keywordId,
        String followUpQuestion,
        String followUpAnswerText
) {}
