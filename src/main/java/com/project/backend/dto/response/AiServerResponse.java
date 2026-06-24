package com.project.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// AI 평가 서버로부터 수신한 오디오 채점 점수, 피드백, STT 및 키워드 매칭 결과를 담는 DTO
public record AiServerResponse(
        Integer score,
        String feedback,
        
        @JsonProperty("transcribed_answer")
        String transcribedAnswer,
        
        @JsonProperty("missing_keywords")
        List<String> missingKeywords,

        @JsonProperty("matched_keywords")
        List<String> matchedKeywords,

        @JsonProperty("captured_image_path")
        String capturedImagePath
) {}
