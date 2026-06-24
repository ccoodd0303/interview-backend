package com.project.backend.dto.request;

import java.util.List;
import java.util.Map;

// 면접 완료 요청 시 비언어적 행동 점수 및 면접 결과 데이터를 제출하는 DTO
public record InterviewCompleteRequest(
        List<Map<String, Object>> answers,
        String mode,
        String type,
        String answerScoreType,
        Map<String, Object> nonverbal
) {}
