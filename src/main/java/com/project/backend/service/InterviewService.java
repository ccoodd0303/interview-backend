package com.project.backend.service;

import com.project.backend.domain.*;
import com.project.backend.dto.request.FollowUpAnswerRequest;
import com.project.backend.dto.request.InterviewCompleteRequest;
import com.project.backend.dto.response.*;
import com.project.backend.repository.*;
import com.project.backend.util.AudioDurationUtil;
import com.project.backend.util.Sm2Algorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {
    private static final int FOLLOW_UP_SKIP_SCORE = 85;

    private static final String EMPTY_KEYWORD_REASON =
            "답변이 인식되지 않아 해당 키워드의 개념 설명을 평가할 수 없습니다.";
    private static final String REPEATED_QUESTION_KEYWORD_REASON =
            "질문 내용을 반복한 답변으로 보여 해당 키워드의 개념 설명으로 인정하기 어렵습니다.";

    private final AiEvaluationService aiEvaluationService;
    private final AnswerLogRepository answerLogRepository;
    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ReviewStateRepository reviewStateRepository;
    private final QuestionService questionService;
    private final TransactionTemplate transactionTemplate;
    private final AudioDurationUtil audioDurationUtil;
    private final KeywordRepository keywordRepository;
    private final AnswerKeywordResultRepository answerKeywordResultRepository;
    private final FollowUpAnswerRepository followUpAnswerRepository;

    public InterviewStartResponse startInterview(Long userId, String subject) {
        return startInterview(userId, subject, "general", InterviewType.GENERAL, QuestionService.SESSION_SIZE);
    }

    public InterviewStartResponse startInterview(Long userId, String subject, String mode,
                                                 InterviewType type, Integer questionCount) {
        synchronized (userId.toString().intern()) {
            return transactionTemplate.execute(status -> {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                List<InterviewSession> activeSessions =
                        sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, SessionStatus.IN_PROGRESS);
                if (!activeSessions.isEmpty()) {
                    sessionRepository.deleteAll(activeSessions);
                    sessionRepository.flush();
                }

                String sessionId = UUID.randomUUID().toString();
                InterviewSession session = InterviewSession.builder()
                        .sessionId(sessionId)
                        .user(user)
                        .subject(subject)
                        .mode(normalizeMode(mode, type))
                        .type(type)
                        .build();
                sessionRepository.saveAndFlush(session);

                List<QuestionResponse> questions = type == InterviewType.ADVANCED
                        ? questionService.getRandomQuestionsBySubject(subject, questionCount != null ? questionCount : 4)
                        : questionService.getQuestionsBySubject(userId, subject);

                if (questions == null || questions.isEmpty()) {
                    throw new IllegalArgumentException("해당 과목의 질문을 찾을 수 없습니다.");
                }

                return new InterviewStartResponse(sessionId, subject, session.getMode(), session.getType().name(), questions);
            });
        }
    }

    @Async
    public void evaluateAnswerAsync(Long userId, String interviewId, Long questionId, Path tempPath) {
        try {
            evaluateAnswer(userId, interviewId, questionId, tempPath, null, null);
        } catch (Exception e) {
            log.error("AI 평가 비동기 처리 중 예외 발생", e);
        }
    }

    public AnswerSubmitResponse evaluateAnswer(Long userId, String interviewId, Long questionId,
                                               Path tempPath, String mode, InterviewType requestType) {
        try {
            Question question = questionRepository.findWithSubjectAndKeywordsById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));

            InterviewSession session = sessionRepository.findBySessionId(interviewId)
                    .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));

            if (!session.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("잘못된 면접 세션 접근입니다.");
            }

            int duration = audioDurationUtil.extractDuration(tempPath);
            Resource audioResource = new FileSystemResource(tempPath.toFile());
            AiServerResponse aiResponse = aiEvaluationService.evaluateAudio(
                    audioResource, question.getSubjectName(),
                    question.getTitle(), question.getIdealAnswer(), question.getTargetKeywords());

            String transcribed = aiResponse.transcribedAnswer();
            Integer score = aiResponse.score();
            if (transcribed == null || transcribed.trim().isEmpty()) {
                transcribed = "(응답 없음)";
                score = 0;
            }

            String confirmedTranscribed = transcribed;
            Integer confirmedScore = score;
            String confirmedScoreReason = aiResponse.scoreReason() != null ? aiResponse.scoreReason() : "";
            InterviewType effectiveType = requestType != null ? requestType : session.getType();

            AnswerLog savedLog = transactionTemplate.execute(status -> {
                InterviewSession activeSession = sessionRepository.findBySessionId(interviewId).orElseThrow();
                AnswerLog log = AnswerLog.builder()
                        .question(question)
                        .userAnswer(confirmedTranscribed)
                        .aiFeedback(aiResponse.feedback() != null ? aiResponse.feedback() : "")
                        .score(confirmedScore)
                        .scoreReason(confirmedScoreReason)
                        .interviewSession(activeSession)
                        .missingKeywords(aiResponse.missingKeywords())
                        .matchedKeywords(aiResponse.matchedKeywords())
                        .capturedImagePath(aiResponse.capturedImagePath())
                        .duration(duration)
                        .build();
                return answerLogRepository.save(log);
            });

            if (effectiveType != InterviewType.ADVANCED) {
                return new AnswerSubmitResponse(savedLog.getId(), questionId, confirmedScore,
                        confirmedScoreReason, confirmedTranscribed, List.of(), null);
            }

            List<Keyword> keywords =
                    keywordRepository.findByQuestionIdOrderByImportanceDescIdAsc(questionId);
            List<AnswerKeywordResultResponse> keywordResults =
                    saveKeywordResults(savedLog, confirmedTranscribed, keywords);
            FollowUpResponse followUp = selectFollowUp(keywords, keywordResults, confirmedScore);

            return new AnswerSubmitResponse(savedLog.getId(), questionId, confirmedScore,
                    confirmedScoreReason, confirmedTranscribed, keywordResults, followUp);
        } finally {
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Transactional
    public FollowUpAnswerResponse saveFollowUpAnswer(String interviewId, FollowUpAnswerRequest request) {
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
        AnswerLog answer = answerLogRepository.findById(request.answerId())
                .orElseThrow(() -> new IllegalArgumentException("답변을 찾을 수 없습니다."));
        if (!answer.getInterviewSession().getId().equals(session.getId())) {
            throw new IllegalArgumentException("잘못된 면접 세션 접근입니다.");
        }
        Keyword keyword = keywordRepository.findById(request.keywordId())
                .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다."));

        Integer score = aiEvaluationService.scoreFollowUpAnswer(
                request.followUpQuestion(), request.followUpAnswerText());
        FollowUpAnswer saved = followUpAnswerRepository.save(FollowUpAnswer.builder()
                .answer(answer)
                .keyword(keyword)
                .followUpQuestion(request.followUpQuestion())
                .followUpAnswerText(request.followUpAnswerText())
                .score(score)
                .build());

        return toFollowUpAnswerResponse(saved);
    }

    public FollowUpAnswerResponse saveFollowUpAnswerAudio(String interviewId, Long answerId, Long keywordId,
                                                          String followUpQuestion, Path tempPath) {
        try {
            InterviewSession session = sessionRepository.findBySessionId(interviewId)
                    .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
            AnswerLog answer = answerLogRepository.findById(answerId)
                    .orElseThrow(() -> new IllegalArgumentException("답변을 찾을 수 없습니다."));
            if (!answer.getInterviewSession().getId().equals(session.getId())) {
                throw new IllegalArgumentException("잘못된 면접 세션 접근입니다.");
            }
            Keyword keyword = keywordRepository.findById(keywordId)
                    .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다."));

            Resource audioResource = new FileSystemResource(tempPath.toFile());
            FollowUpAudioEvaluationResponse evaluation =
                    aiEvaluationService.evaluateFollowUpAudio(audioResource, followUpQuestion);

            String rawTranscribed = evaluation.transcribedAnswer();
            String transcribedAnswer = (rawTranscribed == null || rawTranscribed.trim().isEmpty())
                    ? "(답변 없음)" : rawTranscribed;
            Integer score = evaluation.score() != null ? evaluation.score() : 0;

            FollowUpAnswer saved = transactionTemplate.execute(status ->
                    followUpAnswerRepository.save(FollowUpAnswer.builder()
                            .answer(answer)
                            .keyword(keyword)
                            .followUpQuestion(followUpQuestion)
                            .followUpAnswerText(transcribedAnswer)
                            .score(score)
                            .build())
            );

            return toFollowUpAnswerResponse(saved);
        } finally {
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Transactional
    public InterviewDetailResponse completeInterview(String interviewId) {
        return completeInterview(interviewId, null);
    }

    @Transactional
    public InterviewDetailResponse completeInterview(String interviewId, InterviewCompleteRequest request) {
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));

        List<AnswerLog> answerLogsToSave =
                answerLogRepository.findByInterviewSession_SessionIdOrderByCreatedAtAsc(interviewId);
        if (answerLogsToSave.isEmpty()) {
            throw new IllegalArgumentException("해당 세션에 제출된 답변이 없습니다.");
        }

        InterviewType requestType = request != null ? InterviewType.from(request.type()) : session.getType();
        int expectedCount = requestType == InterviewType.ADVANCED ? 4 : QuestionService.SESSION_SIZE;
        if (answerLogsToSave.size() < expectedCount) {
            throw new IllegalStateException("아직 모든 답변의 AI 채점이 완료되지 않았습니다.");
        }

        String overallFeedback = aiEvaluationService.getOverallSummary(answerLogsToSave);
        int answerAvgScore = calculateAverageScore(answerLogsToSave);
        int avgDuration = calculateAverageDuration(answerLogsToSave);
        Map<String, Object> nonverbal = request != null ? request.nonverbal() : null;
        Integer attitudeScore = extractInteger(nonverbal, "totalScore");
        Integer totalScore = attitudeScore != null
                ? (int) Math.round((answerAvgScore + attitudeScore) / 2.0)
                : answerAvgScore;

        transactionTemplate.executeWithoutResult(status -> {
            InterviewSession activeSession = sessionRepository.findBySessionId(interviewId).orElseThrow();
            activeSession.complete(answerAvgScore, overallFeedback, avgDuration, attitudeScore, totalScore, nonverbal);
            answerLogsToSave.forEach(log ->
                    updateReviewState(activeSession.getUser(), log.getQuestion(), log.getScore()));
        });

        InterviewSession refreshed = sessionRepository.findBySessionId(interviewId).orElseThrow();
        return buildInterviewDetail(refreshed, answerLogsToSave, overallFeedback);
    }

    @Transactional(readOnly = true)
    public InterviewDetailResponse getInterviewResult(String interviewId) {
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
        List<AnswerLog> logs = answerLogRepository.findByInterviewSession_SessionIdOrderByCreatedAtAsc(interviewId);
        return buildInterviewDetail(session, logs, session.getOverallFeedback());
    }

    private List<AnswerKeywordResultResponse> saveKeywordResults(
            AnswerLog savedLog, String transcribed, List<Keyword> keywords) {
        if (isEmptyAnswer(transcribed)) {
            return saveZeroKeywordResults(savedLog, keywords, EMPTY_KEYWORD_REASON);
        }
        if (isRepeatedQuestionAnswer(transcribed)) {
            return saveZeroKeywordResults(savedLog, keywords, REPEATED_QUESTION_KEYWORD_REASON);
        }

        List<AiEvaluationService.KeywordEvaluationRequest> requests = keywords.stream()
                .map(keyword -> new AiEvaluationService.KeywordEvaluationRequest(
                        keyword.getId(),
                        keyword.getKeyword(),
                        keyword.getConceptDescription(),
                        keyword.getFollowUpQuestion(),
                        keyword.getImportance()
                ))
                .toList();
        Map<Long, AiEvaluationService.KeywordEvaluation> evaluations =
                aiEvaluationService.evaluateKeywordConcepts(transcribed, requests).stream()
                        .collect(Collectors.toMap(
                                AiEvaluationService.KeywordEvaluation::keywordId,
                                Function.identity(),
                                (left, right) -> left
                        ));

        return transactionTemplate.execute(status -> keywords.stream()
                .map(keyword -> {
                    AiEvaluationService.KeywordEvaluation evaluation = evaluations.get(keyword.getId());
                    int similarity = evaluation != null && evaluation.similarityScore() != null
                            ? clampScore(evaluation.similarityScore()) : 0;
                    boolean covered = similarity >= 80;
                    String reason = evaluation != null ? evaluation.reason() : "키워드 의미 평가 결과가 없습니다.";
                    answerKeywordResultRepository.save(AnswerKeywordResult.builder()
                            .answer(savedLog)
                            .keyword(keyword)
                            .similarityScore(similarity)
                            .covered(covered)
                            .reason(reason)
                            .build());
                    return new AnswerKeywordResultResponse(
                            keyword.getId(), keyword.getKeyword(), similarity, covered, reason);
                })
                .toList());
    }

    private List<AnswerKeywordResultResponse> saveZeroKeywordResults(
            AnswerLog savedLog, List<Keyword> keywords, String reason) {
        return transactionTemplate.execute(status -> keywords.stream()
                .map(keyword -> {
                    answerKeywordResultRepository.save(AnswerKeywordResult.builder()
                            .answer(savedLog)
                            .keyword(keyword)
                            .similarityScore(0)
                            .covered(false)
                            .reason(reason)
                            .build());
                    return new AnswerKeywordResultResponse(
                            keyword.getId(), keyword.getKeyword(), 0, false, reason);
                })
                .toList());
    }

    private FollowUpResponse selectFollowUp(List<Keyword> keywords,
                                            List<AnswerKeywordResultResponse> keywordResults,
                                            Integer answerScore) {
        if (answerScore != null && answerScore >= FOLLOW_UP_SKIP_SCORE) {
            return null;
        }

        Map<Long, Keyword> keywordMap = keywords.stream()
                .collect(Collectors.toMap(Keyword::getId, Function.identity()));
        return keywordResults.stream()
                .filter(result -> !Boolean.TRUE.equals(result.isCovered()))
                .filter(result -> keywordMap.containsKey(result.keywordId()))
                .min((left, right) -> compareFollowUpPriority(left, right, keywordMap))
                .map(result -> {
                    Keyword keyword = keywordMap.get(result.keywordId());
                    return new FollowUpResponse(
                            keyword.getId(),
                            keyword.getKeyword(),
                            keyword.getFollowUpQuestion(),
                            result.reason()
                    );
                })
                .orElse(null);
    }

    private InterviewDetailResponse buildInterviewDetail(InterviewSession session, List<AnswerLog> logs,
                                                         String feedback) {
        int totalQuestions = logs.size();
        int excellentCount = (int) logs.stream()
                .filter(log -> log.getScore() != null && log.getScore() >= 80)
                .count();
        String date = session.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        List<QuestionDetailResponse> results = logs.stream()
                .map(this::toQuestionDetailResponse)
                .toList();

        Integer answerAvgScore = session.getAvgScore() != null ? session.getAvgScore() : calculateAverageScore(logs);
        Integer totalScore = session.getTotalScore() != null ? session.getTotalScore() : answerAvgScore;

        return new InterviewDetailResponse(
                session.getSessionId(),
                session.getSubject(),
                session.getType().name(),
                "general".equals(session.getMode()) ? "basic" : session.getMode(),
                date,
                answerAvgScore,
                answerAvgScore,
                session.getAttitudeScore(),
                totalScore,
                session.getNonverbal(),
                totalQuestions,
                excellentCount,
                session.getAvgDuration() != null ? session.getAvgDuration() : calculateAverageDuration(logs),
                feedback != null ? feedback : "",
                results);
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
                .map(this::toFollowUpAnswerResponse)
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

    private FollowUpAnswerResponse toFollowUpAnswerResponse(FollowUpAnswer answer) {
        return new FollowUpAnswerResponse(
                answer.getId(),
                answer.getAnswer().getId(),
                answer.getKeyword().getId(),
                answer.getFollowUpQuestion(),
                answer.getFollowUpAnswerText(),
                answer.getScore()
        );
    }

    private int compareFollowUpPriority(AnswerKeywordResultResponse left, AnswerKeywordResultResponse right,
                                        Map<Long, Keyword> keywordMap) {
        int scoreCompare = Integer.compare(
                left.similarityScore() != null ? left.similarityScore() : 0,
                right.similarityScore() != null ? right.similarityScore() : 0
        );
        if (scoreCompare != 0) return scoreCompare;

        Keyword leftKeyword = keywordMap.get(left.keywordId());
        Keyword rightKeyword = keywordMap.get(right.keywordId());
        int importanceCompare = Integer.compare(
                rightKeyword != null ? rightKeyword.getImportance() : 0,
                leftKeyword != null ? leftKeyword.getImportance() : 0
        );
        if (importanceCompare != 0) return importanceCompare;

        return Long.compare(
                left.keywordId() != null ? left.keywordId() : Long.MAX_VALUE,
                right.keywordId() != null ? right.keywordId() : Long.MAX_VALUE
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

    private boolean isEmptyAnswer(String answer) {
        if (answer == null) return true;
        String normalized = answer.trim();
        return normalized.isEmpty()
                || "(답변 없음)".equals(normalized)
                || "(응답 없음)".equals(normalized);
    }

    private boolean isRepeatedQuestionAnswer(String answer) {
        if (answer == null) return false;
        String normalized = answer.replaceAll("[\\s?.!,]", "");
        return normalized.endsWith("무엇인가요")
                || normalized.endsWith("무엇입니까")
                || normalized.endsWith("설명해주세요")
                || normalized.endsWith("말해주세요")
                || normalized.endsWith("알려주세요");
    }

    private int calculateAverageScore(List<AnswerLog> logs) {
        return (int) Math.round(logs.stream()
                .filter(log -> log.getScore() != null)
                .mapToInt(AnswerLog::getScore)
                .average()
                .orElse(0));
    }

    private int calculateAverageDuration(List<AnswerLog> logs) {
        return (int) Math.round(logs.stream()
                .filter(log -> log.getDuration() != null)
                .mapToInt(AnswerLog::getDuration)
                .average()
                .orElse(0));
    }

    private String normalizeMode(String mode, InterviewType type) {
        if (mode != null && !mode.isBlank()) {
            return mode;
        }
        return type == InterviewType.ADVANCED ? "advanced" : "general";
    }

    private Integer extractInteger(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int clampScore(Integer score) {
        if (score == null) return 0;
        return Math.max(0, Math.min(100, score));
    }

    private void updateReviewState(User user, Question question, Integer score) {
        if (score == null) return;
        ReviewState reviewState = reviewStateRepository
                .findByUserIdAndQuestionId(user.getId(), question.getId()).orElse(null);
        LocalDate today = LocalDate.now();

        if (reviewState == null) {
            Sm2Algorithm.Sm2Result sm2Result = Sm2Algorithm.calculate(0, 2.5, 0, score, today, null);
            reviewState = ReviewState.builder().user(user).question(question)
                    .repetitionCount(sm2Result.repetitionCount())
                    .easinessFactor(sm2Result.easinessFactor())
                    .currentInterval(sm2Result.interval())
                    .nextReviewDate(sm2Result.nextReviewDate())
                    .lastReviewedAt(today).build();
        } else {
            Sm2Algorithm.Sm2Result sm2Result = Sm2Algorithm.calculate(
                    reviewState.getRepetitionCount(), reviewState.getEasinessFactor(),
                    reviewState.getCurrentInterval(), score, today, reviewState.getLastReviewedAt());
            reviewState.updateState(sm2Result.repetitionCount(), sm2Result.easinessFactor(),
                    sm2Result.interval(), sm2Result.nextReviewDate(), today);
        }
        reviewStateRepository.save(reviewState);
    }
}
