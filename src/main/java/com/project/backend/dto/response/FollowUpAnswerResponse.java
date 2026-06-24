package com.project.backend.dto.response;

// 제출된 꼬리 질문 답변에 대한 AI 채점 결과를 반환하는 DTO
public record FollowUpAnswerResponse(
        Long id,
        Long answerId,
        Long keywordId,
        String followUpQuestion,
        String answerText,
        Integer score
) {}
