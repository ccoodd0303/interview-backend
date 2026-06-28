package com.project.backend.dto.request;

// 꼬리 질문 답변 제출 요청 데이터
public record FollowUpAnswerRequest(
        Long answerId,
        Long keywordId,
        String followUpQuestion,
        String followUpAnswerText
) {}
