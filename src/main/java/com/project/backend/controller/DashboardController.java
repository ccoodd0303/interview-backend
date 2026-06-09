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

    // 복습 페이지 전체 면접 이력 조회
    @GetMapping("/{userId}/interviews")
    public ResponseEntity<UserInterviewsResponse> getUserInterviews(
            @PathVariable Long userId) {
        
        return ResponseEntity.ok(dashboardService.getUserInterviews(userId));
    }
}
