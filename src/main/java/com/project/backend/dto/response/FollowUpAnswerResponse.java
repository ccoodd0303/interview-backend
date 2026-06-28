package com.project.backend.dto.response;

// 제출된 꼬리 질문 답변에 대한 AI 채점 결과 정보
public record FollowUpAnswerResponse(
        Long id,
        Long answerId,
        Long keywordId,
        String followUpQuestion,
        String answerText,
        Integer score
) {}
