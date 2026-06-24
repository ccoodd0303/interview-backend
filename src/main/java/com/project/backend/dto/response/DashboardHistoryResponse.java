package com.project.backend.dto.response;

import java.util.List;

// 메인 화면 하단 복습하기 리스트에 표시할 면접 이력 요약 데이터를 담는 DTO
public record DashboardHistoryResponse(
        String interviewId,
        String subject,
        String date,
        Integer totalQuestions,
        Integer avgScore,
        String type,
        String mode,
        Integer answerAvgScore,
        Integer attitudeScore,
        Integer totalScore,
        java.util.Map<String, Object> nonverbal,
        List<QuestionDetailResponse> results
) {
    public DashboardHistoryResponse(String interviewId, String subject, String date,
                                    Integer totalQuestions, Integer avgScore) {
        this(interviewId, subject, date, totalQuestions, avgScore, "GENERAL", "basic",
                avgScore, null, avgScore, null, List.of());
    }
}
