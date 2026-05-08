package com.project.backend.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// 점수에 따라 다음 복습 날짜를 계산하는 SM-2 알고리즘 (복습 예상일은 출제 우선 순위 지표로 사용함)
public class Sm2Algorithm {
    
    // 난이도 계수의 최소값 (너무 낮으면 복습 간격이 거의 증가하지 않음)
    private static final double MIN_EF = 1.3;
    
    // 첫 번째 복습 간격
    private static final int FIRST_INTERVAL = 1;
    
    // 두 번째 복습 간격
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
            double easinessFactor, // 체감 난이도
            int currentInterval,   // 기존 복습 주기 (알고리즘에서 이전에 설정했던 일수)
            int score,
            LocalDate today,
            LocalDate lastReviewedDate
    ) {
        
        int q = convertScoreToGrade(score);
        // 낮은 점수면 복습 주기를 다시 짧게 잡음
        if (q < 3) {
            double newEF = easinessFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
            newEF = Math.max(MIN_EF, newEF);
            
            return new Sm2Result(0, newEF, FIRST_INTERVAL,
                    today.plusDays(FIRST_INTERVAL));
        }
        
        // 일정 점수 이상이면 복습 주기를 늘림
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
                // 마지막 복습 후 지난 날짜 계산
                actualInterval = (int) ChronoUnit.DAYS.between(lastReviewedDate, today);
            }
            
            // 늦게 복습했는데도 잘 맞췄다면 실제 지난 기간을 기준으로 계산
            int baseInterval = (q >= 4 && actualInterval > currentInterval) ?
                    actualInterval : currentInterval;
            
            // 당일 반복 학습 시 주기가 0일이 되어 날짜가 고정되는 현상을 방지
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