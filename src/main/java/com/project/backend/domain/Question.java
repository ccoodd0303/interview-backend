package com.project.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "ideal_answer", nullable = false, columnDefinition = "TEXT")
    private String idealAnswer;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Keyword> keywords = new ArrayList<>();

    @Column(name = "difficulty")
    private Short difficulty = (short) 3;

    @Builder
    private Question(Subject subject, String title, String idealAnswer, Short difficulty) {
        this.subject = subject;
        this.title = title;
        this.idealAnswer = idealAnswer;
        this.difficulty = (difficulty != null) ? difficulty : (short) 3;
    }

    public String getSubjectName() {
        return this.subject != null ? this.subject.getName() : "미분류";
    }

    public List<String> getTargetKeywords() {
        if (this.keywords == null) return List.of();
        return this.keywords.stream()
                .map(Keyword::getKeyword)
                .toList();
    }
}
