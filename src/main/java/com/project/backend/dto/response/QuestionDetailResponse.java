package com.project.backend.dto.response;

import java.util.List;

// 복습 화면의 개별 문제 결과
public record QuestionDetailResponse(
        Long questionId,
        String question,
        String answer,
        Integer score,
        Integer duration,
        String feedback,
        List<String> missingKeywords,
        String idealAnswer
) {}