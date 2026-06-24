package com.project.backend.service;

import com.project.backend.domain.InterviewSession;
import com.project.backend.domain.SessionStatus;
import com.project.backend.domain.AnswerKeywordResult;
import com.project.backend.domain.AnswerLog;
import com.project.backend.domain.FollowUpAnswer;
import com.project.backend.dto.response.AnswerKeywordResultResponse;
import com.project.backend.dto.response.DashboardHistoryResponse;
import com.project.backend.dto.response.FollowUpAnswerResponse;
import com.project.backend.dto.response.FollowUpResponse;
import com.project.backend.dto.response.QuestionDetailResponse;
import com.project.backend.dto.response.UserInterviewsResponse;
import com.project.backend.repository.AnswerLogRepository;
import com.project.backend.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final InterviewSessionRepository sessionRepository;
    private final AnswerLogRepository answerLogRepository;
    
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
                .map(session -> {
                    List<AnswerLog> logs = answerLogRepository
                            .findByInterviewSession_SessionIdOrderByCreatedAtAsc(session.getSessionId());
                    return new DashboardHistoryResponse(
                            session.getSessionId(),
                            session.getSubject(),
                            session.getCreatedAt().format(FORMATTER),
                            logs.size(),
                            session.getAvgScore() != null ? session.getAvgScore() : 0,
                            session.getType().name(),
                            "general".equals(session.getMode()) ? "basic" : session.getMode(),
                            session.getAvgScore() != null ? session.getAvgScore() : 0,
                            session.getAttitudeScore(),
                            session.getTotalScore() != null ? session.getTotalScore() : session.getAvgScore(),
                            session.getNonverbal(),
                            List.of()
                    );
                })
                .toList();

        return new UserInterviewsResponse(records);
    }

    private QuestionDetailResponse toQuestionDetailResponse(AnswerLog log) {
        List<AnswerKeywordResultResponse> keywordResults = log.getKeywordResults().stream()
                .map(result -> new AnswerKeywordResultResponse(
                        result.getKeyword().getId(),
                        result.getKeyword().getKeyword(),
                        result.getSimilarityScore(),
                        result.getCovered(),
                        result.getReason()
                ))
                .toList();
        FollowUpResponse followUp = log.getKeywordResults().stream()
                .filter(result -> !Boolean.TRUE.equals(result.getCovered()))
                .min(this::compareFollowUpPriority)
                .map(result -> new FollowUpResponse(
                        result.getKeyword().getId(),
                        result.getKeyword().getKeyword(),
                        result.getKeyword().getFollowUpQuestion(),
                        result.getReason()))
                .orElse(null);
        FollowUpAnswerResponse followUpAnswer = log.getFollowUpAnswers().stream()
                .max(Comparator.comparing(FollowUpAnswer::getId))
                .map(answer -> new FollowUpAnswerResponse(
                        answer.getId(),
                        answer.getAnswer().getId(),
                        answer.getKeyword().getId(),
                        answer.getFollowUpQuestion(),
                        answer.getFollowUpAnswerText(),
                        answer.getScore()
                ))
                .orElse(null);

        return new QuestionDetailResponse(
                log.getQuestion().getId(),
                log.getQuestion().getTitle(),
                log.getUserAnswer(),
                log.getScore(),
                log.getScoreReason(),
                log.getDuration(),
                log.getAiFeedback(),
                log.getMissingKeywords(),
                log.getQuestion().getIdealAnswer(),
                keywordResults,
                followUp,
                followUpAnswer
        );
    }

    private int compareFollowUpPriority(AnswerKeywordResult left, AnswerKeywordResult right) {
        int scoreCompare = Integer.compare(
                left.getSimilarityScore() != null ? left.getSimilarityScore() : 0,
                right.getSimilarityScore() != null ? right.getSimilarityScore() : 0
        );
        if (scoreCompare != 0) return scoreCompare;

        int importanceCompare = Integer.compare(
                right.getKeyword().getImportance() != null ? right.getKeyword().getImportance() : 0,
                left.getKeyword().getImportance() != null ? left.getKeyword().getImportance() : 0
        );
        if (importanceCompare != 0) return importanceCompare;

        return Long.compare(left.getKeyword().getId(), right.getKeyword().getId());
    }
}
