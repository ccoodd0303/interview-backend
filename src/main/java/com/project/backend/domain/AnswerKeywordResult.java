package com.project.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "answer_keyword_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerKeywordResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private AnswerLog answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Column(name = "similarity_score", nullable = false)
    private Integer similarityScore;

    @Column(name = "is_covered", nullable = false)
    private Boolean covered;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Builder
    private AnswerKeywordResult(AnswerLog answer, Keyword keyword, Integer similarityScore,
                                Boolean covered, String reason) {
        this.answer = answer;
        this.keyword = keyword;
        this.similarityScore = similarityScore != null ? similarityScore : 0;
        this.covered = covered != null ? covered : false;
        this.reason = reason != null ? reason : "";
    }
}
