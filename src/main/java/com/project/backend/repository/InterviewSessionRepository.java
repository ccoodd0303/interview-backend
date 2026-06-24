package com.project.backend.repository;

import com.project.backend.domain.InterviewSession;
import com.project.backend.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    
    List<InterviewSession> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, SessionStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s FROM InterviewSession s " +
            "LEFT JOIN FETCH s.answerLogs " +
            "WHERE s.user.id = :userId AND s.status = :status " +
            "ORDER BY s.createdAt DESC")
    List<InterviewSession> findByUserIdAndStatusWithAnswerLogs(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("status") SessionStatus status
    );
    
    Optional<InterviewSession> findBySessionId(String sessionId);
}
