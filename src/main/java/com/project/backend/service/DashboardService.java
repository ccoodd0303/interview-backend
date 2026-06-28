package com.project.backend.service;

import com.project.backend.domain.InterviewSession;
import com.project.backend.domain.SessionStatus;
import com.project.backend.domain.AnswerKeywordResult;
import com.project.backend.domain.AnswerLog;
import com.project.backend.domain.FollowUpAnswer;
import com.project.backend.domain.InterviewType;
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

    // 완료된 면접 이력 목록 조회
    @Transactional(readOnly = true)
    public UserInterviewsResponse getUserInterviews(Long userId) {
        
        List<InterviewSession> allSessions = sessionRepository
                .findByUserIdAndStatusWithAnswerLogs(
                        userId, SessionStatus.COMPLETED);
        
        // 면접 세션 데이터를 이력 화면용 DTO로 변환
        List<DashboardHistoryResponse> records = allSessions.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(session -> new DashboardHistoryResponse(
                        session.getSessionId(),
                        session.getSubject(),
                        session.getCreatedAt().format(FORMATTER),
                        session.getAnswerLogs().size(),
                        session.getAvgScore() != null ? session.getAvgScore() : 0,
                        session.getType().name(),
                        "general".equals(session.getMode()) ? "basic" : session.getMode(),
                        session.getAvgScore() != null ? session.getAvgScore() : 0,
                        session.getAttitudeScore(),
                        session.getTotalScore() != null ? session.getTotalScore() : session.getAvgScore(),
                        session.getNonverbal(),
                        List.of()
                ))
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
        FollowUpResponse followUp = followUpAnswer == null
                ? null
                : log.getKeywordResults().stream()
                .filter(result -> result.getKeyword().getId().equals(followUpAnswer.keywordId()))
                .findFirst()
                .map(result -> new FollowUpResponse(
                        result.getKeyword().getId(),
                        result.getKeyword().getKeyword(),
                        followUpAnswer.followUpQuestion(),
                        result.getReason()))
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

    // 꼬리 질문을 출제할 키워드 우선순위 정렬 (점수가 낮을수록, 중요도가 높을수록, ID가 작을수록 우선)
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
