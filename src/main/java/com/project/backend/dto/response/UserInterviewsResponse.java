package com.project.backend.dto.response;

import java.util.List;

// 마이페이지 및 복습용 면접 기록 목록 조회 데이터
public record UserInterviewsResponse(
        List<DashboardHistoryResponse> records
) {}
