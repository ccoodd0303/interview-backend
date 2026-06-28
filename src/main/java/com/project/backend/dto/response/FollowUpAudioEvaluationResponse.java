package com.project.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// AI 서버로부터 수신한 꼬리 질문 음성 답변 평가 결과 정보
public record FollowUpAudioEvaluationResponse(
        @JsonProperty("transcribed_answer")
        String transcribedAnswer,
        Integer score
) {}
