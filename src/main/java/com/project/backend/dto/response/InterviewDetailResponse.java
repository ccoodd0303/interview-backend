package com.project.backend.dto.response;

import java.util.List;
import java.util.Map;

// 면접 상세 결과 및 종합 피드백 정보 (비언어적 지표 포함)
public record InterviewDetailResponse(
        String interviewId,
        String subject,
        String type,
        String mode,
        String date,
        Integer avgScore,
        Integer answerAvgScore,
        Integer attitudeScore,
        Integer totalScore,
        Map<String, Object> nonverbal,
        Integer totalQuestions,
        Integer excellentCount,
        Integer avgTime,
        String feedback,
        List<QuestionDetailResponse> results
) {
    public InterviewDetailResponse(String interviewId, String subject, String date, Integer avgScore,
                                   Integer totalQuestions, Integer excellentCount, Integer avgTime,
                                   String feedback, List<QuestionDetailResponse> results) {
        this(interviewId, subject, "GENERAL", "basic", date, avgScore, avgScore,
                null, avgScore, null, totalQuestions, excellentCount, avgTime, feedback, results);
    }
}
