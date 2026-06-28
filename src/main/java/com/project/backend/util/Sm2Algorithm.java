package com.project.backend.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// SM-2 알고리즘으로 점수별 다음 복습 날짜 계산 (복습 주기로 출제 우선순위 결정)
public class Sm2Algorithm {
    
    // 난이도 계수의 최솟값 (이보다 낮아지면 복습 간격이 늘어나지 않음)
    private static final double MIN_EF = 1.3;
    
    // 1차 복습 주기
    private static final int FIRST_INTERVAL = 1;
    
    // 2차 복습 주기
    private static final int SECOND_INTERVAL = 6;
    
    public static int convertScoreToGrade(int score) {
        if (score >= 80) return 5;
        if (score >= 60) return 4;
        if (score >= 40) return 3;
        if (score >= 20) return 2;
        return 1;
    }
    
    public static Sm2Result calculate(
            int repetitionCount,   // 연속 정답 횟수
            double easinessFactor, // 체감 난이도 계수
            int currentInterval,   // 기존 복습 주기 (일수)
            int score,
            LocalDate today,
            LocalDate lastReviewedDate
    ) {
        
        int q = convertScoreToGrade(score);
        // 점수가 낮으면 복습 주기를 다시 단축
        if (q < 3) {
            double newEF = easinessFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
            newEF = Math.max(MIN_EF, newEF);
            
            return new Sm2Result(0, newEF, FIRST_INTERVAL,
                    today.plusDays(FIRST_INTERVAL));
        }
        
        // 점수가 높으면 복습 주기를 연장
        double newEF = easinessFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        newEF = Math.max(MIN_EF, newEF);
        
        int newRepetitionCount = repetitionCount + 1;
        
        int nextInterval;
        
        if (newRepetitionCount == 1) {
            nextInterval = FIRST_INTERVAL;
        } else if (newRepetitionCount == 2) {
            nextInterval = SECOND_INTERVAL;
        } else {
            int actualInterval = currentInterval;
            
            if (lastReviewedDate != null) {
                // 마지막 복습 시점부터 경과한 일수 계산
                actualInterval = (int) ChronoUnit.DAYS.between(lastReviewedDate, today);
            }
            
            // 복습 기한을 넘겨 완료했으나 점수가 높다면, 실제 지연된 일수를 반영해 주기 계산
            int baseInterval = (q >= 4 && actualInterval > currentInterval) ?
                    actualInterval : currentInterval;
            
            // 하루에 여러 번 학습해도 주기가 0일로 고정되는 현상 방지
            baseInterval = Math.max(1, baseInterval);
            
            nextInterval = (int) Math.ceil(baseInterval * newEF);
        }
        
        return new Sm2Result(newRepetitionCount, newEF, nextInterval,
                today.plusDays(nextInterval));
    }
    
    // 계산 결과
    public record Sm2Result(
            int repetitionCount,      // 갱신된 연속 정답 횟수
            double easinessFactor,    // 갱신된 체감 난이도 계수
            int interval,             // 갱신된 다음 복습 주기 (일수)
            LocalDate nextReviewDate  // 계산된 다음 복습 예정 날짜
    ) {}
}