package com.project.backend.service;

import com.project.backend.domain.AnswerLog;
import com.project.backend.domain.InterviewSession;
import com.project.backend.domain.Question;
import com.project.backend.domain.ReviewState;
import com.project.backend.domain.SessionStatus;
import com.project.backend.domain.User;
import com.project.backend.dto.request.AnswerSubmitRequest;
import com.project.backend.dto.response.AiServerResponse;
import com.project.backend.dto.response.AnswerEvaluationResponse;
import com.project.backend.dto.response.InterviewDetailResponse;
import com.project.backend.dto.response.InterviewStartResponse;
import com.project.backend.dto.response.InterviewSummaryResponse;
import com.project.backend.dto.response.QuestionDetailResponse;
import com.project.backend.dto.response.QuestionResponse;
import com.project.backend.repository.AnswerLogRepository;
import com.project.backend.repository.InterviewSessionRepository;
import com.project.backend.repository.QuestionRepository;
import com.project.backend.repository.ReviewStateRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.util.Sm2Algorithm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
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
            throw new IllegalArgumentException("No questions found");
        }
        
        return new InterviewStartResponse(sessionId, category, questions);
    }
    
    
    // 개별 문제에 대한 음성 답변을 AI 서버로 전송하여 채점
    public AnswerEvaluationResponse evaluateAnswer(
            Long userId, String interviewId, Long questionId,
            MultipartFile audioFile) {
        
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
        
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized session access");
        }
        
        AiServerResponse aiResponse = aiEvaluationService.evaluateAudio(
                audioFile, question.getCategory(),
                question.getQuestionText(), question.getTargetKeywords());
        
        String transcribed = aiResponse.transcribedAnswer();
        Integer score = aiResponse.score();
        
        if (transcribed == null || transcribed.trim().isEmpty()) {
            transcribed = "(응답 없음)";
            score = 0;
        }
        
        return new AnswerEvaluationResponse(
                questionId, transcribed, score,
                aiResponse.feedback(), aiResponse.missingKeywords());
    }
    
    
    // 면접 완료 시 답변 기록을 일괄 저장하고 AI 서버에 전달해서 총평 받기
    @Transactional
    public InterviewSummaryResponse completeInterview(
            String interviewId, List<AnswerSubmitRequest> answers) {
        
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        List<Long> questionIds = answers.stream()
                .map(AnswerSubmitRequest::questionId).toList();
        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds)
                .stream().collect(Collectors.toMap(Question::getId, q -> q));
        
        List<AnswerLog> answerLogsToSave = answers.stream().map(ans -> {
            Question question = questionMap.get(ans.questionId());
            return AnswerLog.builder()
                    .question(question)
                    .userAnswer(ans.transcribedAnswer())
                    .aiFeedback(ans.feedback())
                    .score(ans.score())
                    .interviewSession(session)
                    .missingKeywords(ans.missingKeywords())
                    .build();
        }).toList();
        
        String overallFeedback = aiEvaluationService.getOverallSummary(answerLogsToSave);
        
        answerLogRepository.saveAll(answerLogsToSave);
        
        int avgScore = (int) Math.round(answers.stream()
                .mapToInt(AnswerSubmitRequest::score).average().orElse(0));
        
        // 면접 세션을 완료 상태로 변경하고
        session.complete(avgScore, overallFeedback);
        
        // 복습 주기 갱신
        answerLogsToSave.forEach(log ->
                updateReviewState(session.getUser(), log.getQuestion(), log.getScore()));
        
        String date = session.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        
        return new InterviewSummaryResponse(
                session.getSessionId(), session.getCategory(),
                date, avgScore, overallFeedback);
    }
    
    
    // 복습 화면과 세부 결과창에 쓸 데이터 처리
    @Transactional(readOnly = true)
    public InterviewDetailResponse getInterviewResult(String interviewId) {
        
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
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