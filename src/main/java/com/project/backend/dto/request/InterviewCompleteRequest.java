package com.project.backend.dto.request;

import java.util.List;
import java.util.Map;

// 면접 완료 요청 데이터
public record InterviewCompleteRequest(
        List<Map<String, Object>> answers,
        String mode,
        String type,
        String answerScoreType,
        Map<String, Object> nonverbal
) {}
