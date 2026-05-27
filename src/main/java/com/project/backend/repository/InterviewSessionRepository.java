package com.project.backend.repository;

import com.project.backend.domain.InterviewSession;
import com.project.backend.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    
    List<InterviewSession> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, SessionStatus status);
    
    Optional<InterviewSession> findBySessionId(String sessionId);
    
    @Modifying
    @Query("DELETE FROM InterviewSession s WHERE s.user.id = :userId AND s.status = :status")
    void deleteByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SessionStatus status);
}
