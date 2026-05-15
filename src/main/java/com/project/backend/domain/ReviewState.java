package com.project.backend.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "review_state",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_state_user_question",
                        columnNames = {"user_id", "question_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    // 반복 학습 횟수
    @Column(nullable = false)
    private Integer repetitionCount;
    
    // 문제 난이도 계수
    @Column(nullable = false)
    private Double easinessFactor;
    
    // 현재 복습 주기
    @Column(nullable = false)
    private Integer currentInterval;
    
    @Column(nullable = false)
    private LocalDate nextReviewDate;
    
    @Column(nullable = false)
    private LocalDate lastReviewedAt;
    
    @Builder
    private ReviewState(User user, Question question, Integer repetitionCount,
                        Double easinessFactor, Integer currentInterval,
                        LocalDate nextReviewDate, LocalDate lastReviewedAt) {
        this.user = user;
        this.question = question;
        this.repetitionCount = repetitionCount;
        this.easinessFactor = easinessFactor;
        this.currentInterval = currentInterval;
        this.nextReviewDate = nextReviewDate;
        this.lastReviewedAt = lastReviewedAt;
    }
    
    // 채점 결과로 복습 상태 갱신
    public void updateState(Integer repetitionCount, Double easinessFactor,
                            Integer currentInterval, LocalDate nextReviewDate,
                            LocalDate now) {
        
        // 갱신 날짜가 없으면 잘못된 요청으로 처리
        if (now == null) {
            throw new IllegalArgumentException("갱신 날짜가 누락되었습니다.");
        }
        this.repetitionCount = repetitionCount;
        this.easinessFactor = easinessFactor;
        this.currentInterval = currentInterval;
        this.nextReviewDate = nextReviewDate;
        this.lastReviewedAt = now;
    }
    
    
}
