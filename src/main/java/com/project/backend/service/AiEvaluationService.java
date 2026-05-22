package com.project.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            Resource audioResource, String category, String questionText, List<String> targetKeywords) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audioFile", audioResource);
            body.add("category", category);
            body.add("question", questionText);
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
            
            return restClient.post()
                    .uri("/summary")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            
        } catch (Exception e) {
            log.error("AI 서버 총평 요청 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("AI 서버 통신 중 오류가 발생했습니다.");
        }
    }
}