package com.project.backend.dto.response;

// 메인 화면 하단 복습하기 리스트에 표시할 데이터
public record DashboardHistoryResponse(
        String interviewId,
        String subject,
        String date,
        Integer totalQuestions,
        Integer avgScore
) {}
