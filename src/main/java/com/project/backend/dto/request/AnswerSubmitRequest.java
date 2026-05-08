package com.project.backend.dto.request;

import java.util.List;

public record AnswerSubmitRequest(
        Long questionId,
        String transcribedAnswer,
        Integer score,
        String feedback,
        List<String> missingKeywords
) {}