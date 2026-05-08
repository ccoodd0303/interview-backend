package com.project.backend.dto.request;

import java.util.List;

public record InterviewCompleteRequest(
        List<AnswerSubmitRequest> answers
) {}