package com.project.backend.service;

import com.project.backend.domain.Question;
import com.project.backend.domain.ReviewState;
import com.project.backend.dto.response.QuestionResponse;
import com.project.backend.repository.QuestionRepository;
import com.project.backend.repository.ReviewStateRepository;
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
    
    
    // 과목별 문제를 SM-2 알고리즘의 복습 우선순위에 맞추어 조회
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsBySubject(
            Long userId, String subject) {
        
        List<Question> availableQuestions =
                questionRepository.findBySubjectName(subject);
        
        List<ReviewState> reviewStates = reviewStateRepository
                .findByUserIdAndQuestionSubject(userId, subject);
        
        Map<Long, ReviewState> reviewStateMap = reviewStates.stream()
                .collect(Collectors.toMap(
                        state -> state.getQuestion().getId(),
                        state -> state
                ));
        
        // 문제를 복습 대상, 신규 문제, 복습 대기 중인 문제로 분류
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
        
        
        // 그룹별 정렬 (복습 대상은 과거순, 신규는 랜덤, 대기 중인 문제는 기한 임박순)
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
                        q.getSubjectName(),
                        q.getTitle()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getRandomQuestionsBySubject(String subject, int questionCount) {
        int limit = questionCount > 0 ? questionCount : 4;
        return questionRepository.findRandomBySubjectName(subject, limit).stream()
                .map(q -> new QuestionResponse(
                        q.getId(),
                        q.getSubjectName(),
                        q.getTitle()
                ))
                .toList();
    }
}
