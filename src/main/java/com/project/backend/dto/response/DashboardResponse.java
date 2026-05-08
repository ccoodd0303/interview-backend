package com.project.backend.dto.response;

import java.util.List;
import java.util.Map;

// 메인 화면 전체를 그리기 위한 데이터
public record DashboardResponse(
        int totalAttempts,
        int availableCategories,
        Map<String, Integer> categoryRecentScores, // 상단 카테고리 카드용
        List<DashboardHistoryResponse> records     // 하단 복습 리스트용
) {}