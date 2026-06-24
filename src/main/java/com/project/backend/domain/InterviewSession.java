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
import java.util.Map;

@Entity
@Table(name = "interview_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 36)
    private String sessionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 100)
    private String subject;

    @Column(nullable = false, length = 20)
    private String mode = "general";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewType type = InterviewType.GENERAL;
    
    @Column
    private Integer avgScore;
    
    @Column(columnDefinition = "TEXT")
    private String overallFeedback;
    
    @Column
    private Integer avgDuration;

    @Column
    private Integer attitudeScore;

    @Column
    private Integer totalScore;

    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> nonverbal;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.IN_PROGRESS;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "interviewSession", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<AnswerLog> answerLogs = new ArrayList<>();
    
    @Builder
    private InterviewSession(String sessionId, User user, String subject, String mode, InterviewType type) {
        this.sessionId = sessionId;
        this.user = user;
        this.subject = subject;
        this.mode = (mode == null || mode.isBlank()) ? "general" : mode;
        this.type = type != null ? type : InterviewType.GENERAL;
        this.status = SessionStatus.IN_PROGRESS;
    }
    
    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
    
    // 면접 완료 처리
    public void complete(Integer avgScore, String overallFeedback, Integer avgDuration) {
        this.avgScore = avgScore;
        this.overallFeedback = overallFeedback;
        this.avgDuration = avgDuration;
        this.status = SessionStatus.COMPLETED;
    }

    public void complete(Integer avgScore, String overallFeedback, Integer avgDuration,
                         Integer attitudeScore, Integer totalScore,
                         Map<String, Object> nonverbal) {
        complete(avgScore, overallFeedback, avgDuration);
        this.attitudeScore = attitudeScore;
        this.totalScore = totalScore;
        this.nonverbal = nonverbal;
    }
}
