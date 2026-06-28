package com.project.backend.controller;

import com.project.backend.dto.response.UserInterviewsResponse;
import com.project.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;

    // 사용자의 전체 면접 기록 조회 (마이페이지/복습용)
    @GetMapping("/{userId}/interviews")
    public ResponseEntity<UserInterviewsResponse> getUserInterviews(
            @PathVariable Long userId) {
        
        return ResponseEntity.ok(dashboardService.getUserInterviews(userId));
    }
}
