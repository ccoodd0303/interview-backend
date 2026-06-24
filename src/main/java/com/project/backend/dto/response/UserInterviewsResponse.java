package com.project.backend.dto.response;

import java.util.List;

// 마이페이지/복습 페이지 등에서 사용자의 전체 면접 이력 목록 조회를 위한 DTO
public record UserInterviewsResponse(
        List<DashboardHistoryResponse> records
) {}
