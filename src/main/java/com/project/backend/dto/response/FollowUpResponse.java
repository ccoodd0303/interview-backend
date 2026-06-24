package com.project.backend.dto.response;

// 부족한 키워드가 있을 때 출제할 꼬리 질문과 대상 키워드 정보를 담는 DTO
public record FollowUpResponse(
        Long keywordId,
        String keyword,
        String question
) {}
