package com.project.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "follow_up_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FollowUpAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private AnswerLog answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Column(name = "follow_up_question", nullable = false, columnDefinition = "TEXT")
    private String followUpQuestion;

    @Column(name = "follow_up_answer_text", nullable = false, columnDefinition = "TEXT")
    private String followUpAnswerText;

    @Column(nullable = false)
    private Integer score;

    @Builder
    private FollowUpAnswer(AnswerLog answer, Keyword keyword, String followUpQuestion,
                           String followUpAnswerText, Integer score) {
        this.answer = answer;
        this.keyword = keyword;
        this.followUpQuestion = followUpQuestion;
        this.followUpAnswerText = followUpAnswerText;
        this.score = score != null ? score : 0;
    }
}
