package com.project.backend.dto.response;

import java.util.List;

public record AnswerSubmitResponse(
        Long answerId,
        Long questionId,
        Integer score,
        String scoreReason,
        String transcribedAnswer,
        List<AnswerKeywordResultResponse> answerKeywordResults,
        FollowUpResponse followUp
) {}
