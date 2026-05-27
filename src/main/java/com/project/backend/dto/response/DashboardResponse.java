package com.project.backend.dto.response;

import java.util.Map;

// 메인 화면용 데이터
public record DashboardResponse(
        int totalAttempts,
        int availableSubjects,
        Map<String, Integer> subjectRecentScores
) {}
