package com.project.backend.service;

import com.project.backend.domain.InterviewSession;
import com.project.backend.domain.SessionStatus;
import com.project.backend.dto.response.DashboardHistoryResponse;
import com.project.backend.dto.response.UserInterviewsResponse;
import com.project.backend.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final InterviewSessionRepository sessionRepository;
    
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");

    // 복습 페이지 전체 면접 이력 조회
    @Transactional(readOnly = true)
    public UserInterviewsResponse getUserInterviews(Long userId) {
        
        List<InterviewSession> allSessions = sessionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, SessionStatus.COMPLETED);
        
        // 모든 면접 세션을 복습 이력 DTO로 변환
        List<DashboardHistoryResponse> records = allSessions.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(session -> new DashboardHistoryResponse(
                        session.getSessionId(),
                        session.getSubject(),
                        session.getCreatedAt().format(FORMATTER),
                        QuestionService.SESSION_SIZE,
                        session.getAvgScore() != null ? session.getAvgScore() : 0
                ))
                .toList();

        return new UserInterviewsResponse(records);
    }
}
