package com.project.backend.dto.response;

// 면접 10문제 완료 직후 띄워주는 최종 결과 요약창
public record InterviewSummaryResponse(
        String interviewId,
        String category,
        String date,
        Integer avgScore,
        String overallFeedback
) {}