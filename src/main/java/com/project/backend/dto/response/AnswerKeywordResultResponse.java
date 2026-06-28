package com.project.backend.dto.response;

// 답변 내 각 키워드별 LLM 의미 유사도 점수와 커버 여부, 부족 사유 전달 정보
public record AnswerKeywordResultResponse(
        Long keywordId,
        String keyword,
        Integer similarityScore,
        Boolean isCovered,
        String reason
) {}
