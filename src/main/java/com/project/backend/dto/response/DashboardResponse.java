package com.project.backend.dto.response;

import java.util.List;
import java.util.Map;

// 메인 화면 전체를 그리기 위한 데이터
public record DashboardResponse(
        int totalAttempts,
        int availableSubjects,
        Map<String, Integer> subjectRecentScores,
        List<DashboardHistoryResponse> records     // 하단 복습 리스트용
) {}
