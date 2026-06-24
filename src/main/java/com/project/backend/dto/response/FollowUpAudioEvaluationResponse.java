package com.project.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FollowUpAudioEvaluationResponse(
        @JsonProperty("transcribed_answer")
        String transcribedAnswer,
        Integer score
) {}
