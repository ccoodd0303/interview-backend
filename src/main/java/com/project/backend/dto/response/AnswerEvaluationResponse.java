package com.project.backend.dto.response;

import java.util.List;

// 벡엔드 -> 프론트엔드로 AI 채점 결과
public record AnswerEvaluationResponse(
        Long questionId,
        String transcribedAnswer,
        Integer score,
        String feedback,
        List<String> missingKeywords
) {}