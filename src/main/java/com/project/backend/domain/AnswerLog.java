package com.project.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    
    @Column(name = "user_answer", nullable = false, columnDefinition = "TEXT")
    private String userAnswer;
    
    @Column(name = "ai_feedback", nullable = false, columnDefinition = "TEXT")
    private String aiFeedback;
    
    @Column
    private Integer score;

    @Column(name = "score_reason", columnDefinition = "TEXT")
    private String scoreReason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession interviewSession;
    
    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "missing_keywords", columnDefinition = "jsonb")
    private List<String> missingKeywords;
    
    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "matched_keywords", columnDefinition = "jsonb")
    private List<String> matchedKeywords;

    @Column(name = "captured_image_path", length = 255)
    private String capturedImagePath;
    
    @Column
    private Integer duration;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<AnswerKeywordResult> keywordResults = new ArrayList<>();

    @OneToMany(mappedBy = "answer", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<FollowUpAnswer> followUpAnswers = new ArrayList<>();
    
    @Builder
    private AnswerLog(Question question, String userAnswer, String aiFeedback,
                      Integer score, String scoreReason, InterviewSession interviewSession,
                      List<String> missingKeywords, List<String> matchedKeywords, // String -> List<String>으로 변경
                      String capturedImagePath, Integer duration) {
        this.question = question;
        this.userAnswer = userAnswer;
        this.aiFeedback = aiFeedback;
        this.score = score;
        this.scoreReason = scoreReason != null ? scoreReason : "";
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
