package com.project.backend.service;

import com.project.backend.domain.*;
import com.project.backend.dto.response.*;
import com.project.backend.repository.*;
import com.project.backend.util.Sm2Algorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {
    
    private final AiEvaluationService aiEvaluationService;
    private final AnswerLogRepository answerLogRepository;
    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ReviewStateRepository reviewStateRepository;
    private final QuestionService questionService;
    
    
    // 새로운 면접 세션을 생성하고 지정된 카테고리의 문제를 출제
    @Transactional
    public InterviewStartResponse startInterview(Long userId, String category) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        sessionRepository.deleteByUserIdAndStatus(
                userId, SessionStatus.IN_PROGRESS);
        
        String sessionId = UUID.randomUUID().toString();
        InterviewSession session = InterviewSession.builder()
                .sessionId(sessionId).user(user).category(category).build();
        sessionRepository.save(session);
        
        // 유저의 복습 상태(SM-2)에 맞춰 우선순위가 높은 10개의 문제 추출
        List<QuestionResponse> questions =
                questionService.getQuestionsByCategory(userId, category);
        
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("해당 카테고리의 질문을 찾을 수 없습니다.");
        }
        
        return new InterviewStartResponse(sessionId, category, questions);
    }
    
    
    // 개별 문제에 대한 음성 답변을 비동기로 AI 서버에 전송하여 채점 후 DB 저장
    @Async
    @Transactional
    public void evaluateAnswerAsync(
            Long userId, String interviewId, Long questionId,
            byte[] audioBytes, String filename) {
        
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));
        
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
        
        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("잘못된 면접 세션 접근입니다.");
        }
        
        Resource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        
        AiServerResponse aiResponse = aiEvaluationService.evaluateAudio(
                audioResource, question.getCategory(),
                question.getQuestionText(), question.getTargetKeywords());
        
        String transcribed = aiResponse.transcribedAnswer();
        Integer score = aiResponse.score();
        
        if (transcribed == null || transcribed.trim().isEmpty()) {
            transcribed = "(응답 없음)";
            score = 0;
        }
        
        AnswerLog log = AnswerLog.builder()
                .question(question)
                .userAnswer(transcribed)
                .aiFeedback(aiResponse.feedback())
                .score(score)
                .interviewSession(session)
                .missingKeywords(aiResponse.missingKeywords())
                .build();
                
        answerLogRepository.save(log);
    }
    
    
    // 면접 완료 시 저장된 답변 기록을 바탕으로 AI 서버에 전달해서 총평 받기
    @Transactional
    public InterviewDetailResponse completeInterview(String interviewId) {
        
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
        
        List<AnswerLog> answerLogsToSave = answerLogRepository
                .findByInterviewSession_SessionIdOrderByCreatedAtAsc(interviewId);
                
        if (answerLogsToSave.isEmpty()) {
            throw new IllegalArgumentException("해당 세션에 제출된 답변이 없습니다.");
        }
        
        String overallFeedback = aiEvaluationService.getOverallSummary(answerLogsToSave);
        
        int avgScore = (int) Math.round(answerLogsToSave.stream()
                .filter(log -> log.getScore() != null)
                .mapToInt(AnswerLog::getScore).average().orElse(0));
        
        // 면접 세션을 완료 상태로 변경
        session.complete(avgScore, overallFeedback);
        
        // 복습 주기 갱신
        answerLogsToSave.forEach(log ->
                updateReviewState(session.getUser(), log.getQuestion(), log.getScore()));
        
        String date = session.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                
        // 10문제 상세 결과 반환
        int totalQuestions = answerLogsToSave.size();
        int excellentCount = (int) answerLogsToSave.stream()
                .filter(log -> log.getScore() != null && log.getScore() >= 80)
                .count();
                
        List<QuestionDetailResponse> results = answerLogsToSave.stream().map(log ->
                new QuestionDetailResponse(
                        log.getQuestion().getId(),
                        log.getQuestion().getQuestionText(),
                        log.getUserAnswer(),
                        log.getScore(),
                        log.getAiFeedback(),
                        log.getMissingKeywords()
                )
        ).toList();
        
        return new InterviewDetailResponse(
                session.getSessionId(), session.getCategory(),
                date, avgScore, totalQuestions, excellentCount, results);
    }
    
    // 면접 세션 중단 및 관련 데이터 삭제
    @Transactional
    public void deleteSession(String interviewId) {
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
        answerLogRepository.deleteByInterviewSession_SessionId(interviewId);
        sessionRepository.delete(session);
    }
    
    
    // 복습 화면과 세부 결과창에 쓸 데이터 처리
    @Transactional(readOnly = true)
    public InterviewDetailResponse getInterviewResult(String interviewId) {
        
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
        
        List<AnswerLog> logs = answerLogRepository
                .findByInterviewSession_SessionIdOrderByCreatedAtAsc(interviewId);
        
        int totalQuestions = logs.size();
        int excellentCount = (int) logs.stream()
                .filter(log -> log.getScore() != null && log.getScore() >= 80)
                .count();
        
        List<QuestionDetailResponse> results = logs.stream().map(log ->
                new QuestionDetailResponse(
                        log.getQuestion().getId(),
                        log.getQuestion().getQuestionText(),
                        log.getUserAnswer(),
                        log.getScore(),
                        log.getAiFeedback(),
                        log.getMissingKeywords()
                )
        ).toList();
        
        String date = session.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        
        return new InterviewDetailResponse(
                session.getSessionId(), session.getCategory(),
                date, session.getAvgScore(),
                totalQuestions, excellentCount, results);
    }
    
    
    // 문제 채점 결과로 다음 복습 주기를 갱신
    private void updateReviewState(User user, Question question, Integer score) {
        if (score == null) return;
        ReviewState reviewState = reviewStateRepository
                .findByUserIdAndQuestionId(user.getId(), question.getId()).orElse(null);
        LocalDate today = LocalDate.now();
        
        // 처음 푼 경우 초기값
        if (reviewState == null) {
            Sm2Algorithm.Sm2Result sm2Result = Sm2Algorithm.calculate(
                    0, 2.5, 0, score, today, null);
            reviewState = ReviewState.builder().user(user).question(question)
                    .repetitionCount(sm2Result.repetitionCount())
                    .easinessFactor(sm2Result.easinessFactor())
                    .currentInterval(sm2Result.interval())
                    .nextReviewDate(sm2Result.nextReviewDate())
                    .lastReviewedAt(today).build();
        } 
        
        // 이미 푼 경우 기존 데이터 기반으로 계산
        else {
            Sm2Algorithm.Sm2Result sm2Result = Sm2Algorithm.calculate(
                    reviewState.getRepetitionCount(), reviewState.getEasinessFactor(),
                    reviewState.getCurrentInterval(), score, today,
                    reviewState.getLastReviewedAt());
                    reviewState.updateState(sm2Result.repetitionCount(), sm2Result.easinessFactor(),
                    sm2Result.interval(), sm2Result.nextReviewDate(), today);
        }
        reviewStateRepository.save(reviewState);
    }
}