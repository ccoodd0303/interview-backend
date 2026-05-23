package com.project.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "answer_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @Column(name = "user_answer", nullable = false, columnDefinition = "TEXT")
    private String userAnswer;
    
    @Column(name = "ai_feedback", nullable = false, columnDefinition = "TEXT")
    private String aiFeedback;
    
    @Column
    private Integer score;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession interviewSession;
    
    @Column(name = "missing_keywords", length = 255)
    private String missingKeywords;

    @Column(name = "matched_keywords", length = 255)
    private String matchedKeywords;

    @Column(name = "captured_image_path", length = 255)
    private String capturedImagePath;
    
    @Column
    private Integer duration;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Builder
    private AnswerLog(Question question, String userAnswer, String aiFeedback,
                      Integer score, InterviewSession interviewSession,
                      String missingKeywords, String matchedKeywords,
                      String capturedImagePath, Integer duration) {
        this.question = question;
        this.userAnswer = userAnswer;
        this.aiFeedback = aiFeedback;
        this.score = score;
        this.interviewSession = interviewSession;
        this.missingKeywords = missingKeywords;
        this.matchedKeywords = matchedKeywords;
        this.capturedImagePath = capturedImagePath;
        this.duration = duration;
    }
    
    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
