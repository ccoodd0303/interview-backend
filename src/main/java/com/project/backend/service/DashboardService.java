package com.project.backend.service;

import com.project.backend.domain.InterviewSession;
import com.project.backend.domain.SessionStatus;
import com.project.backend.dto.response.DashboardHistoryResponse;
import com.project.backend.dto.response.DashboardResponse;
import com.project.backend.repository.InterviewSessionRepository;
import com.project.backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");
    
    // 메인 화면 전체를 위한 데이터 처리
    @Transactional(readOnly = true)
    public DashboardResponse getUserDashboard(Long userId) {
        
        // 유저의 면접 이력 전부 불러와서
        List<InterviewSession> allSessions = sessionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, SessionStatus.COMPLETED);
        
        List<String> categories = questionRepository.findDistinctCategories();
        
        Map<String, Integer> recentScores = new LinkedHashMap<>();
        List<DashboardHistoryResponse> recentReviews = new ArrayList<>();
        
        // 카테고리별 가장 최근 면접 1건 추려내기
        for (String category : categories) {
            allSessions.stream()
                    .filter(session -> session.getCategory().equals(category))
                    .findFirst()
                    .ifPresent(session -> {
                        recentScores.put(category, session.getAvgScore());
                        
                        recentReviews.add(new DashboardHistoryResponse(
                                session.getSessionId(),
                                session.getCategory(),
                                session.getCreatedAt().format(FORMATTER),
                                QuestionService.SESSION_SIZE,
                                session.getAvgScore() != null ?
                                        session.getAvgScore() : 0
                        ));
                    });
            
            recentScores.putIfAbsent(category, null);
        }
        
        return new DashboardResponse(
                allSessions.size(), // 총 응시 횟수
                categories.size(), // 선택 가능 주제
                recentScores, // 면접 주제 선택창의 최근 점수
                recentReviews // 복습하기 창의 최근 면접 이력
        );
    }
    
    // 복습하기의 카테고리 리스트 화면으로 이동할 때
    // 해당 카테고리의 면접 이력을 출력하기 위한 데이터 처리
    @Transactional(readOnly = true)
    public List<DashboardHistoryResponse> getInterviewsByCategory(
            Long userId, String category) {
        
        // 완료된 면접 이력 전부 불러와서
        List<InterviewSession> sessions = sessionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, SessionStatus.COMPLETED);
        
        // 카테고리에 해당하는 것만 추리기
        return sessions.stream()
                .filter(session -> session.getCategory().equals(category))
                .map(session -> new DashboardHistoryResponse(
                        session.getSessionId(),
                        session.getCategory(),
                        session.getCreatedAt().format(FORMATTER),
                        QuestionService.SESSION_SIZE,
                        session.getAvgScore() != null ? session.getAvgScore() : 0
                ))
                .toList();
    }
}