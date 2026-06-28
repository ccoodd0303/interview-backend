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

// 모의 면접 비즈니스 로직 처리
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {
    // 이 점수 이상이면 꼬리 질문을 생성하지 않음
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

    // 일반 모드 면접을 기본 설정으로 시작
    public InterviewStartResponse startInterview(Long userId, String subject) {
        return startInterview(userId, subject, "general", InterviewType.GENERAL, QuestionService.SESSION_SIZE);
    }

    // 면접 세션 생성 및 세션 타입(일반/심화)에 따른 질문 구성
    public InterviewStartResponse startInterview(Long userId, String subject, String mode,
                                                 InterviewType type, Integer questionCount) {
        // 동시 요청으로 인한 세션 중복 생성 방지
        synchronized (userId.toString().intern()) {
            return transactionTemplate.execute(status -> {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                // 사용자는 한 번에 하나의 면접만 진행 가능 -> 기존 진행 중 세션 삭제
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

                // 심화 모드는 랜덤 질문, 일반 모드는 맞춤 질문 구성
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

    // 답변 평가 비동기 처리
    @Async
    public void evaluateAnswerAsync(Long userId, String interviewId, Long questionId, Path tempPath) {
        try {
            evaluateAnswer(userId, interviewId, questionId, tempPath, null, null);
        } catch (Exception e) {
            log.error("AI 평가 비동기 처리 중 예외 발생", e);
        }
    }

    // 답변 평가 (STT, 채점, 심화 모드 처리)
    public AnswerSubmitResponse evaluateAnswer(Long userId, String interviewId, Long questionId,
                                               Path tempPath, String mode, InterviewType requestType) {
        try {
            Question question = questionRepository.findWithSubjectAndKeywordsById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));

            InterviewSession session = sessionRepository.findBySessionId(interviewId)
                    .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));

            // 다른 사용자의 세션에 답변 제출하는 것을 방지
            if (!session.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("잘못된 면접 세션 접근입니다.");
            }

            int duration = audioDurationUtil.extractDuration(tempPath);
            Resource audioResource = new FileSystemResource(tempPath.toFile());
            // AI 서버에 오디오 채점 요청
            AiServerResponse aiResponse = aiEvaluationService.evaluateAudio(
                    audioResource, question.getSubjectName(),
                    question.getTitle(), question.getIdealAnswer(), question.getTargetKeywords());

            String transcribed = aiResponse.transcribedAnswer();
            Integer score = aiResponse.score();
            // STT 결과가 없으면(무음 등) 응답 없음으로 간주하고 0점 처리
            if (transcribed == null || transcribed.trim().isEmpty()) {
                transcribed = "(응답 없음)";
                score = 0;
            }

            String confirmedTranscribed = transcribed;
            Integer confirmedScore = score;
            String confirmedScoreReason = aiResponse.scoreReason() != null ? aiResponse.scoreReason() : "";
            // requestType 누락 시 세션 타입 기준 적용
            InterviewType effectiveType = requestType != null ? requestType : session.getType();

            // 채점 결과 저장
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

            // 일반 모드는 키워드 평가/꼬리 질문 없이 바로 응답
            if (effectiveType != InterviewType.ADVANCED) {
                return new AnswerSubmitResponse(savedLog.getId(), questionId, confirmedScore,
                        confirmedScoreReason, confirmedTranscribed, List.of(), null);
            }

            // 심화 모드용 키워드 평가 및 꼬리 질문 선정
            List<Keyword> keywords =
                    keywordRepository.findByQuestionIdOrderByImportanceDescIdAsc(questionId);
            List<AnswerKeywordResultResponse> keywordResults =
                    saveKeywordResults(savedLog, confirmedTranscribed, keywords);
            FollowUpResponse followUp = selectFollowUp(keywords, keywordResults, confirmedScore);

            return new AnswerSubmitResponse(savedLog.getId(), questionId, confirmedScore,
                    confirmedScoreReason, confirmedTranscribed, keywordResults, followUp);
        } finally {
            // 업로드된 임시 오디오 파일은 성공/실패와 무관하게 항상 정리
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    // 텍스트로 입력된 꼬리 질문 답변 채점 및 저장
    @Transactional
    public FollowUpAnswerResponse saveFollowUpAnswer(String interviewId, FollowUpAnswerRequest request) {
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
        AnswerLog answer = answerLogRepository.findById(request.answerId())
                .orElseThrow(() -> new IllegalArgumentException("답변을 찾을 수 없습니다."));
        // 꼬리 질문이 속한 원본 답변이 현재 세션의 것인지 검증
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

    // 음성으로 입력된 꼬리 질문 답변을 STT 변환 후 채점 및 저장
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
            // STT 결과가 없으면 답변 없음으로 처리
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

    // 비언어적 평가 없이 면접 세션 완료 처리
    @Transactional
    public InterviewDetailResponse completeInterview(String interviewId) {
        return completeInterview(interviewId, null);
    }

    // 면접 세션 완료 처리 (피드백 생성, 총점 계산, 복습 상태 반영)
    @Transactional
    public InterviewDetailResponse completeInterview(String interviewId, InterviewCompleteRequest request) {
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));

        List<AnswerLog> answerLogsToSave =
                answerLogRepository.findByInterviewSession_SessionIdOrderByCreatedAtAsc(interviewId);
        if (answerLogsToSave.isEmpty()) {
            throw new IllegalArgumentException("해당 세션에 제출된 답변이 없습니다.");
        }

        // 모드별 기대 문항 수 검증
        InterviewType requestType = request != null ? InterviewType.from(request.type()) : session.getType();
        int expectedCount = requestType == InterviewType.ADVANCED ? 4 : QuestionService.SESSION_SIZE;
        // 비동기 채점이 아직 끝나지 않은 답변이 있으면 완료 처리를 막음
        if (answerLogsToSave.size() < expectedCount) {
            throw new IllegalStateException("아직 모든 답변의 AI 채점이 완료되지 않았습니다.");
        }

        String overallFeedback = aiEvaluationService.getOverallSummary(answerLogsToSave);
        int answerAvgScore = calculateAverageScore(answerLogsToSave);
        int avgDuration = calculateAverageDuration(answerLogsToSave);
        Map<String, Object> nonverbal = request != null ? request.nonverbal() : null;
        // 비언어적 평가 반영하여 최종 총점 계산
        Integer attitudeScore = extractInteger(nonverbal, "totalScore");
        Integer totalScore = attitudeScore != null
                ? (int) Math.round((answerAvgScore + attitudeScore) / 2.0)
                : answerAvgScore;

        transactionTemplate.executeWithoutResult(status -> {
            InterviewSession activeSession = sessionRepository.findBySessionId(interviewId).orElseThrow();
            activeSession.complete(answerAvgScore, overallFeedback, avgDuration, attitudeScore, totalScore, nonverbal);
            // 질문별로 이번 점수를 반영해 다음 복습 시점 재계산
            answerLogsToSave.forEach(log ->
                    updateReviewState(activeSession.getUser(), log.getQuestion(), log.getScore()));
        });

        InterviewSession refreshed = sessionRepository.findBySessionId(interviewId).orElseThrow();
        return buildInterviewDetail(refreshed, answerLogsToSave, overallFeedback);
    }

    // 완료 여부와 관계없이 현재까지의 답변 기록으로 면접 상세 결과 조회
    @Transactional(readOnly = true)
    public InterviewDetailResponse getInterviewResult(String interviewId) {
        InterviewSession session = sessionRepository.findBySessionId(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
        List<AnswerLog> logs = answerLogRepository.findByInterviewSession_SessionIdOrderByCreatedAtAsc(interviewId);
        return buildInterviewDetail(session, logs, session.getOverallFeedback());
    }

    // 키워드별 개념 설명 여부 평가 및 저장
    private List<AnswerKeywordResultResponse> saveKeywordResults(
            AnswerLog savedLog, String transcribed, List<Keyword> keywords) {
        // 답변이 비었거나 질문 복사형인 경우 0점 처리
        if (isEmptyAnswer(transcribed)) {
            return saveZeroKeywordResults(savedLog, keywords, EMPTY_KEYWORD_REASON);
        }
        if (isRepeatedQuestionAnswer(transcribed)) {
            return saveZeroKeywordResults(savedLog, keywords, REPEATED_QUESTION_KEYWORD_REASON);
        }

        // 키워드 목록을 AI 평가 요청 형식으로 변환
        List<AiEvaluationService.KeywordEvaluationRequest> requests = keywords.stream()
                .map(keyword -> new AiEvaluationService.KeywordEvaluationRequest(
                        keyword.getId(),
                        keyword.getKeyword(),
                        keyword.getConceptDescription(),
                        keyword.getFollowUpQuestion(),
                        keyword.getImportance()
                ))
                .toList();
        // 평가 결과를 keywordId 기준 맵으로 변환
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
                    // 유사도 80점 이상이면 해당 키워드 개념을 충분히 설명한 것으로 간주
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

    // 모든 키워드에 동일한 사유로 0점/미커버 결과를 일괄 저장 (AI 호출 불필요한 케이스용)
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

    // 꼬리 질문 대상 키워드 선정
    private FollowUpResponse selectFollowUp(List<Keyword> keywords,
                                            List<AnswerKeywordResultResponse> keywordResults,
                                            Integer answerScore) {
        if (answerScore != null && answerScore >= FOLLOW_UP_SKIP_SCORE) {
            return null;
        }

        Map<Long, Keyword> keywordMap = keywords.stream()
                .collect(Collectors.toMap(Keyword::getId, Function.identity()));
        // 누락된(covered=false) 키워드 중 우선순위가 가장 높은 것을 선택
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

    // 면접 상세 결과 데이터 구성
    private InterviewDetailResponse buildInterviewDetail(InterviewSession session, List<AnswerLog> logs,
                                                         String feedback) {
        int totalQuestions = logs.size();
        // 80점 이상을 "우수 답변"으로 집계
        int excellentCount = (int) logs.stream()
                .filter(log -> log.getScore() != null && log.getScore() >= 80)
                .count();
        String date = session.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        List<QuestionDetailResponse> results = logs.stream()
                .map(this::toQuestionDetailResponse)
                .toList();

        // 미완료 세션인 경우 실시간 집계
        Integer answerAvgScore = session.getAvgScore() != null ? session.getAvgScore() : calculateAverageScore(logs);
        Integer totalScore = session.getTotalScore() != null ? session.getTotalScore() : answerAvgScore;

        return new InterviewDetailResponse(
                session.getSessionId(),
                session.getSubject(),
                session.getType().name(),
                // 과거 데이터 호환을 위해 "general" 모드는 "basic"으로 변환해 응답
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

    // AnswerLog -> DTO 변환
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
        // 가장 최신의 꼬리 질문 답변 채택
        FollowUpAnswerResponse followUpAnswer = log.getFollowUpAnswers().stream()
                .max(Comparator.comparing(FollowUpAnswer::getId))
                .map(this::toFollowUpAnswerResponse)
                .orElse(null);
        // 최신 꼬리 질문 답변이 어느 키워드에 대한 것인지 찾아 FollowUpResponse로 재구성
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

    // FollowUpAnswer 엔티티 -> 응답 DTO 매핑
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

    // 꼬리 질문 우선순위 비교 (유사도 낮은 순 > 중요도 높은 순 > 키워드 ID 순)
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

    // compareFollowUpPriority(DTO 버전)와 동일 로직의 엔티티 버전 (현재 호출처 없음)
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

    // 답변 텍스트가 비어 있거나 '응답 없음' 형태인지 판별
    private boolean isEmptyAnswer(String answer) {
        if (answer == null) return true;
        String normalized = answer.trim();
        return normalized.isEmpty()
                || "(답변 없음)".equals(normalized)
                || "(응답 없음)".equals(normalized);
    }

    // 질문의 종결 어미로 끝나는 복사형 답변인지 확인
    private boolean isRepeatedQuestionAnswer(String answer) {
        if (answer == null) return false;
        String normalized = answer.replaceAll("[\\s?.!,]", "");
        return normalized.endsWith("무엇인가요")
                || normalized.endsWith("무엇입니까")
                || normalized.endsWith("설명해주세요")
                || normalized.endsWith("말해주세요")
                || normalized.endsWith("알려주세요");
    }

    // 평균 답변 점수 계산
    private int calculateAverageScore(List<AnswerLog> logs) {
        return (int) Math.round(logs.stream()
                .filter(log -> log.getScore() != null)
                .mapToInt(AnswerLog::getScore)
                .average()
                .orElse(0));
    }

    // 평균 답변 시간 계산
    private int calculateAverageDuration(List<AnswerLog> logs) {
        return (int) Math.round(logs.stream()
                .filter(log -> log.getDuration() != null)
                .mapToInt(AnswerLog::getDuration)
                .average()
                .orElse(0));
    }

    // 기본 모드 결정
    private String normalizeMode(String mode, InterviewType type) {
        if (mode != null && !mode.isBlank()) {
            return mode;
        }
        return type == InterviewType.ADVANCED ? "advanced" : "general";
    }

    // 안전한 Integer 변환 및 추출
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

    // SM-2 알고리즘 기반 복습 일정 갱신
    private void updateReviewState(User user, Question question, Integer score) {
        if (score == null) return;
        ReviewState reviewState = reviewStateRepository
                .findByUserIdAndQuestionId(user.getId(), question.getId()).orElse(null);
        LocalDate today = LocalDate.now();

        if (reviewState == null) {
            // 첫 복습: 반복 0회, 기본 용이도(EF) 2.5에서 시작
            Sm2Algorithm.Sm2Result sm2Result = Sm2Algorithm.calculate(0, 2.5, 0, score, today, null);
            reviewState = ReviewState.builder().user(user).question(question)
                    .repetitionCount(sm2Result.repetitionCount())
                    .easinessFactor(sm2Result.easinessFactor())
                    .currentInterval(sm2Result.interval())
                    .nextReviewDate(sm2Result.nextReviewDate())
                    .lastReviewedAt(today).build();
        } else {
            // 기존 복습 상태 + 이번 점수로 다음 복습일 등 재계산
            Sm2Algorithm.Sm2Result sm2Result = Sm2Algorithm.calculate(
                    reviewState.getRepetitionCount(), reviewState.getEasinessFactor(),
                    reviewState.getCurrentInterval(), score, today, reviewState.getLastReviewedAt());
            reviewState.updateState(sm2Result.repetitionCount(), sm2Result.easinessFactor(),
                    sm2Result.interval(), sm2Result.nextReviewDate(), today);
        }
        reviewStateRepository.save(reviewState);
    }
}
