package com.project.backend.dto.response;

import java.util.List;

public record InterviewStartResponse(
        String interviewId,
        String category,
        List<QuestionResponse> questions
) {}