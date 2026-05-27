package com.project.backend.controller;

import com.project.backend.dto.response.DashboardHistoryResponse;
import com.project.backend.dto.response.DashboardResponse;
import com.project.backend.dto.response.UserInterviewsResponse;
import com.project.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    // 메인 화면 대시보드 데이터 처리
    @GetMapping("/{userId}/dashboard")
    public ResponseEntity<DashboardResponse> getUserDashboardStats(
            @PathVariable Long userId) {
        
        return ResponseEntity.ok(dashboardService.getUserDashboardStats(userId));
    }

    // 복습 페이지 전체 면접 이력 조회
    @GetMapping("/{userId}/interviews")
    public ResponseEntity<UserInterviewsResponse> getUserInterviews(
            @PathVariable Long userId) {
        
        return ResponseEntity.ok(dashboardService.getUserInterviews(userId));
    }
    
    // 복습하기에서 과목별 면접 이력 리스트 출력하기 위한 데이터 처리
    @GetMapping("/{userId}/interviews/subject")
    public ResponseEntity<List<DashboardHistoryResponse>> getInterviewsBySubject(
            @PathVariable Long userId,
            @RequestParam("subject") String subject) {
        
        return ResponseEntity.ok(
                dashboardService.getInterviewsBySubject(userId, subject)
        );
    }
}
