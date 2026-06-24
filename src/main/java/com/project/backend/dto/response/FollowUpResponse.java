package com.project.backend.dto.response;

public record FollowUpResponse(
        Long keywordId,
        String keyword,
        String question,
        String reason
) {}
