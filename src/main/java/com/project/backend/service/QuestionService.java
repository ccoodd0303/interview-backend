package com.project.backend.service;

import com.project.backend.domain.ReviewState;
import com.project.backend.domain.Question;
import com.project.backend.dto.response.QuestionResponse;
import com.project.backend.repository.ReviewStateRepository;
import com.project.backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    private final ReviewStateRepository reviewStateRepository;
    
    public static final int SESSION_SIZE = 10;
    
    
    // 카테고리의 문제들을 복습 알고리즘(SM-2) 우선순위에 따라 출제
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByCategory(
            Long userId, String category) {
        
        List<Question> availableQuestions =
                questionRepository.findByCategory(category);
        
        List<ReviewState> reviewStates = reviewStateRepository
                .findByUserIdAndQuestionCategory(userId, category);
        
        Map<Long, ReviewState> reviewStateMap = reviewStates.stream()
                .collect(Collectors.toMap(
                        state -> state.getQuestion().getId(),
                        state -> state
                ));
        
        // 복습 기한이 된 문제, 처음 푸는 문제, 아직 기한이 안 된 문제로 분류
        List<Question> dueReviews = new ArrayList<>();
        List<Question> newQuestions = new ArrayList<>();
        List<Question> earlyReviews = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        
        for (Question question : availableQuestions) {
            ReviewState state = reviewStateMap.get(question.getId());
            
            if (state == null) {
                newQuestions.add(question);
            } else {
                LocalDate nextReviewDate = state.getNextReviewDate();
                if (!nextReviewDate.isAfter(today)) {
                    dueReviews.add(question);
                } else {
                    earlyReviews.add(question);
                }
            }
        }
        
        // 복습 기한이 된 문제는 오래된 순, 신규 문제는 랜덤, 기한 안 된 문제는 임박한 순 정렬
        dueReviews.sort(Comparator.comparing(
                q -> reviewStateMap.get(q.getId()).getNextReviewDate()
        ));
        
        Collections.shuffle(newQuestions);
        
        earlyReviews.sort(Comparator.comparing(
                q -> reviewStateMap.get(q.getId()).getNextReviewDate()
        ));
        
        List<Question> finalQueue = new ArrayList<>();
        
        // 출제 우선순위에 따라 문제 선정
        for (Question q : dueReviews) {
            if (finalQueue.size() >= SESSION_SIZE) break;
            finalQueue.add(q);
        }
        
        for (Question q : newQuestions) {
            if (finalQueue.size() >= SESSION_SIZE) break;
            finalQueue.add(q);
        }
        
        for (Question q : earlyReviews) {
            if (finalQueue.size() >= SESSION_SIZE) break;
            finalQueue.add(q);
        }
        
        return finalQueue.stream()
                .map(q -> new QuestionResponse(
                        q.getId(),
                        q.getCategory(),
                        q.getQuestionText()
                ))
                .toList();
    }
}