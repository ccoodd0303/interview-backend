package com.project.backend.repository;

import com.project.backend.domain.AnswerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerLogRepository extends JpaRepository<AnswerLog, Long> {
    
    @Query("SELECT a FROM AnswerLog a JOIN FETCH a.question " +
            "WHERE a.interviewSession.sessionId = :sessionId " +
            "ORDER BY a.createdAt ASC")
    List<AnswerLog> findByInterviewSession_SessionIdOrderByCreatedAtAsc(
            @Param("sessionId") String sessionId
    );
    
    void deleteByInterviewSession_SessionId(String sessionId);
}