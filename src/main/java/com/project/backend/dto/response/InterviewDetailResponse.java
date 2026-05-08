package com.project.backend.dto.response;

import java.util.List;

// 복습 화면과 세부 결과창 데이터
public record InterviewDetailResponse(
        String interviewId,
        String category,
        String date,
        Integer avgScore,
        Integer totalQuestions,
        Integer excellentCount,
        List<QuestionDetailResponse> results
) {}