package com.project.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    
    @Column(nullable = false, length = 50)
    private String subject;
    
    @Column
    private Integer avgScore;
    
    @Column(columnDefinition = "TEXT")
    private String overallFeedback;
    
    @Column
    private Integer avgDuration;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.IN_PROGRESS;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "interviewSession", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<AnswerLog> answerLogs = new ArrayList<>();
    
    @Builder
    private InterviewSession(String sessionId, User user, String subject) {
        this.sessionId = sessionId;
        this.user = user;
        this.subject = subject;
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
}
