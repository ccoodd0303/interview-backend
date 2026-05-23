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

    @Column(name = "weight")
    private Integer weight = 1;

    @Builder
    private Keyword(Question question, String keyword, Integer weight) {
        this.question = question;
        this.keyword = keyword;
        this.weight = (weight != null) ? weight : 1;
    }
}
