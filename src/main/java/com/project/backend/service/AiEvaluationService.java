package com.project.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.backend.domain.AnswerLog;
import com.project.backend.dto.response.AiServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiEvaluationService {
    
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    
    public AiEvaluationService(@Value("${ai.server.url}") String aiServerUrl, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        
        // 연결 5초, 응답 60초 타임아웃 (AI 서버 처리 시간 고려)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));
        
        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(factory)
                .build();
    }
    
    // 음성 파일과 문제 정보를 AI 서버로 전송하고 채점 결과 받기
    public AiServerResponse evaluateAudio(
            Resource audioResource, String subject, String questionText, String idealAnswer, List<String> targetKeywords) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audioFile", audioResource);
            body.add("subject", subject);
            body.add("question", questionText);
            body.add("ideal_answer", idealAnswer);
            if (targetKeywords != null && !targetKeywords.isEmpty()) {
                body.add("target_keywords", objectMapper.writeValueAsString(targetKeywords));
            }
            
            return restClient.post()
                    .uri("/evaluate")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(AiServerResponse.class);
            
        } catch (Exception e) {
            log.error("AI 서버 통신 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("AI 서버 통신 중 오류가 발생했습니다.");
        }
    }
    
    // 면접 전체 답변을 AI 서버로 전송하고 면접 총평 받기
    public String getOverallSummary(List<AnswerLog> logs) {
        try {
            List<Map<String, Object>> answers = logs.stream()
                    .map(log -> Map.<String, Object>of(
                            "question", log.getQuestion().getTitle(),
                            "answer", log.getUserAnswer() != null ? log.getUserAnswer() : "",
                            "score", log.getScore() != null ? log.getScore() : 0
                    ))
                    .toList();
            
            String body = objectMapper.writeValueAsString(Map.of("answers", answers));
            
            String rawResponse = restClient.post()
                    .uri("/summary")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            
            return objectMapper.readValue(rawResponse, String.class);
            
        } catch (Exception e) {
            log.error("AI 서버 총평 요청 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("AI 서버 통신 중 오류가 발생했습니다.");
        }
    }

    public List<KeywordEvaluation> evaluateKeywordConcepts(
            String answerText, List<KeywordEvaluationRequest> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        try {
            Map<String, Object> body = Map.of(
                    "answer", answerText != null ? answerText : "",
                    "keywords", keywords
            );

            String rawResponse = restClient.post()
                    .uri("/evaluate-keywords")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            KeywordEvaluation[] response =
                    objectMapper.readValue(rawResponse, KeywordEvaluation[].class);
            return List.of(response);
        } catch (Exception e) {
            log.warn("AI 서버 키워드 의미 평가 실패, 보수적 대체 평가 사용: {}", e.getMessage());
            return keywords.stream()
                    .map(keyword -> fallbackKeywordEvaluation(answerText, keyword))
                    .toList();
        }
    }

    public Integer scoreFollowUpAnswer(String question, String answerText) {
        try {
            Map<String, Object> body = Map.of(
                    "question", question != null ? question : "",
                    "answer", answerText != null ? answerText : ""
            );

            String rawResponse = restClient.post()
                    .uri("/evaluate-follow-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            Map<?, ?> response = objectMapper.readValue(rawResponse, Map.class);
            Object score = response.get("score");
            if (score instanceof Number number) {
                return clampScore(number.intValue());
            }
            return clampScore(Integer.parseInt(String.valueOf(score)));
        } catch (Exception e) {
            log.warn("AI 서버 꼬리답변 채점 실패, 대체 점수 사용: {}", e.getMessage());
            int length = answerText != null ? answerText.trim().length() : 0;
            return length >= 40 ? 80 : length >= 15 ? 60 : 30;
        }
    }

    private KeywordEvaluation fallbackKeywordEvaluation(String answerText, KeywordEvaluationRequest keyword) {
        String answer = answerText != null ? answerText : "";
        String concept = keyword.conceptDescription() != null ? keyword.conceptDescription() : "";
        long conceptTokenMatches = java.util.Arrays.stream(concept.split("[\\s,./()]+"))
                .filter(token -> token.length() >= 2)
                .filter(answer::contains)
                .count();
        boolean keywordOnly = answer.contains(keyword.keyword()) && conceptTokenMatches == 0;
        int score = keywordOnly ? 45 : (int) Math.min(95, conceptTokenMatches * 25);
        return new KeywordEvaluation(
                keyword.keywordId(),
                clampScore(score),
                clampScore(score) >= 80,
                keywordOnly
                        ? "키워드는 언급했지만 개념 설명이 부족합니다."
                        : "AI 서버 의미 평가를 사용할 수 없어 개념 설명의 핵심 표현 일치도를 기준으로 임시 평가했습니다."
        );
    }

    private Integer clampScore(Integer score) {
        if (score == null) return 0;
        return Math.max(0, Math.min(100, score));
    }

    public record KeywordEvaluationRequest(
            @JsonProperty("keyword_id")
            Long keywordId,
            String keyword,
            @JsonProperty("concept_description")
            String conceptDescription,
            @JsonProperty("follow_up_question")
            String followUpQuestion,
            Integer importance
    ) {}

    public record KeywordEvaluation(
            @JsonProperty("keyword_id")
            Long keywordId,
            @JsonProperty("similarity_score")
            Integer similarityScore,
            @JsonProperty("is_covered")
            Boolean isCovered,
            String reason
    ) {}
}
