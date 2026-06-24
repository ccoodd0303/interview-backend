package com.project.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @Column(name = "importance")
    private Integer importance = 1;

    @Column(name = "concept_description", columnDefinition = "TEXT")
    private String conceptDescription;

    @Column(name = "follow_up_question", columnDefinition = "TEXT")
    private String followUpQuestion;

    @Builder
    private Keyword(Question question, String keyword, Integer importance,
                    String conceptDescription, String followUpQuestion) {
        this.question = question;
        this.keyword = keyword;
        this.importance = (importance != null) ? importance : 1;
        this.conceptDescription = conceptDescription;
        this.followUpQuestion = followUpQuestion;
    }
}
