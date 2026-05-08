package com.project.backend.repository;

import com.project.backend.domain.ReviewState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewStateRepository extends JpaRepository<ReviewState, Long> {
    
    Optional<ReviewState> findByUserIdAndQuestionId(Long userId, Long questionId);
    
    @Query("SELECT rs FROM ReviewState rs JOIN FETCH rs.question WHERE rs.user.id = :userId AND rs.question.category = :category")
    List<ReviewState> findByUserIdAndQuestionCategory(@Param("userId") Long userId, @Param("category") String category);
}
