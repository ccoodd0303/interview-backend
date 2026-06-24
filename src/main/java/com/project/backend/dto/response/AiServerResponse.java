package com.project.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// AI 서버 -> 백엔드로 AI 채점 결과
public record AiServerResponse(
        Integer score,
        @JsonProperty("score_reason")
        String scoreReason,
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
