package com.project.backend.dto.response;

public record QuestionResponse(
        Long questionId,
        String subject,
        String questionText
) {}
