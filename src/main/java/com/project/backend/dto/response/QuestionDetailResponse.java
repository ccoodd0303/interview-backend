package com.project.backend.dto.response;

import java.util.List;

// 복습 화면의 개별 문제 결과 정보
public record QuestionDetailResponse(
        Long questionId,
        String question,
        String answer,
        Integer score,
        String scoreReason,
        Integer duration,
        String feedback,
        List<String> missingKeywords,
        String idealAnswer,
        List<AnswerKeywordResultResponse> answerKeywordResults,
        FollowUpResponse followUp,
        FollowUpAnswerResponse followUpAnswer
) {
    public QuestionDetailResponse(Long questionId, String question, String answer, Integer score,
                                  Integer duration, String feedback, List<String> missingKeywords,
                                  String idealAnswer) {
        this(questionId, question, answer, score, "", duration, feedback, missingKeywords, idealAnswer,
                List.of(), null, null);
    }
}
