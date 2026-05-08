package com.project.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;

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
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String userAnswer;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String aiFeedback;
    
    @Column
    private Integer score;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession interviewSession;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> missingKeywords;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Builder
    private AnswerLog(Question question, String userAnswer, String aiFeedback,
                      Integer score, InterviewSession interviewSession,
                      List<String> missingKeywords) {
        this.question = question;
        this.userAnswer = userAnswer;
        this.aiFeedback = aiFeedback;
        this.score = score;
        this.interviewSession = interviewSession;
        this.missingKeywords = missingKeywords;
    }
    
    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}