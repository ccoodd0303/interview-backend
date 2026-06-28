package com.project.backend.dto.response;

import java.util.List;

// 개별 질문에 대한 음성 답변 제출 시 즉각적으로 반환되는 채점 및 피드백 정보
public record AnswerSubmitResponse(
        Long answerId,
        Long questionId,
        Integer score,
        String scoreReason,
        String transcribedAnswer,
        List<AnswerKeywordResultResponse> answerKeywordResults,
        FollowUpResponse followUp
) {}
