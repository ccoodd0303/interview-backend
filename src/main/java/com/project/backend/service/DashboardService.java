package com.project.backend.service;

import com.project.backend.domain.InterviewSession;
import com.project.backend.domain.SessionStatus;
import com.project.backend.dto.response.DashboardHistoryResponse;
import com.project.backend.dto.response.DashboardResponse;
import com.project.backend.dto.response.UserInterviewsResponse;
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
    
    // 메인 화면 대시보드 데이터 처리
    @Transactional(readOnly = true)
    public DashboardResponse getUserDashboardStats(Long userId) {
        
        // 데이터 조회
        List<InterviewSession> allSessions = sessionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, SessionStatus.COMPLETED);
        
        List<String> subjects = questionRepository.findDistinctSubjects();
        
        // 최근 점수 계산
        Map<String, Integer> recentScores = new LinkedHashMap<>();
        for (String subject : subjects) {
            allSessions.stream()
                    .filter(session -> session.getSubject().equals(subject))
                    .findFirst()
                    .ifPresent(session -> {
                        recentScores.put(subject, session.getAvgScore());
                    });
            
            recentScores.putIfAbsent(subject, null);
        }
        
        return new DashboardResponse(
                allSessions.size(), // 총 응시 횟수
                subjects.size(), // 선택 가능 주제
                recentScores // 면접 주제 선택창의 최근 점수
        );
    }

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
    
    // 복습하기의 과목 리스트 화면으로 이동할 때
    // 해당 과목의 면접 이력을 출력하기 위한 데이터 처리
    @Transactional(readOnly = true)
    public List<DashboardHistoryResponse> getInterviewsBySubject(
            Long userId, String subject) {
        
        // 완료된 면접 이력 전부 불러와서
        List<InterviewSession> sessions = sessionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, SessionStatus.COMPLETED);
        
        // 과목에 해당하는 것만 추리기
        return sessions.stream()
                .filter(session -> session.getSubject().equals(subject))
                .map(session -> new DashboardHistoryResponse(
                        session.getSessionId(),
                        session.getSubject(),
                        session.getCreatedAt().format(FORMATTER),
                        QuestionService.SESSION_SIZE,
                        session.getAvgScore() != null ? session.getAvgScore() : 0
                ))
                .toList();
    }
}
