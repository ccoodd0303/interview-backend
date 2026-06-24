package com.project.backend.dto.response;

import java.util.List;
import java.util.Map;

// 면접의 상세 채점 결과와 종합 피드백, 비언어적 점수를 반환하는 DTO
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
