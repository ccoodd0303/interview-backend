package com.project.backend.repository;

import com.project.backend.domain.InterviewSession;
import com.project.backend.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    
    List<InterviewSession> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, SessionStatus status);
    
    Optional<InterviewSession> findBySessionId(String sessionId);
    
}
