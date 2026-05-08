package com.project.backend.dto.response;

public record QuestionResponse(
        Long questionId,
        String category,
        String questionText
) {}