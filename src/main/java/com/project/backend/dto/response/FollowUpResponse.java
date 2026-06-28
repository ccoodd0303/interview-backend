package com.project.backend.dto.response;

// 심화 면접에서 출제할 꼬리 질문과 매칭되는 키워드 정보
public record FollowUpResponse(
        Long keywordId,
        String keyword,
        String question,
        String reason
) {}
