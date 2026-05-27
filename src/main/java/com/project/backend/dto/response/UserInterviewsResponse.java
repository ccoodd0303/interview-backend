package com.project.backend.dto.response;

import java.util.List;

// 복습 페이지 전체 면접 이력 조회용 DTO
public record UserInterviewsResponse(
        List<DashboardHistoryResponse> records
) {}
