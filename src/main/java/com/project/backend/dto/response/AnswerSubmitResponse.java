package com.project.backend.dto.response;

import java.util.List;

// 답변 제출 시 AI 채점 결과와 상세 키워드 분석 결과, 꼬리 질문(존재 시) 정보를 반환하는 DTO
public record AnswerSubmitResponse(
        Long answerId,
        Long questionId,
        Integer score,
        String transcribedAnswer,
        List<AnswerKeywordResultResponse> answerKeywordResults,
        FollowUpResponse followUp
) {}
