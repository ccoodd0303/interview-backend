package com.project.backend.dto.response;

import java.util.List;

// 면접 세부 결과창에서 개별 문제의 메인 답변/꼬리 질문에 대한 점수 및 피드백을 전달하는 DTO
public record QuestionDetailResponse(
        Long questionId,
        String question,
        String answer,
        Integer score,
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
        this(questionId, question, answer, score, duration, feedback, missingKeywords, idealAnswer,
                List.of(), null, null);
    }
}
