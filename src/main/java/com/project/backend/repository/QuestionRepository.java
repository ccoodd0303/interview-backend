package com.project.backend.repository;

import com.project.backend.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    @Query("SELECT q FROM Question q JOIN FETCH q.subject WHERE q.subject.name = :subject")
    List<Question> findBySubjectName(@Param("subject") String subject);

    @Query(value = """
            SELECT q.* FROM questions q
            JOIN subjects s ON q.subject_id = s.id
            WHERE s.name = :subject
            ORDER BY RANDOM()
            LIMIT :limit
            """, nativeQuery = true)
    List<Question> findRandomBySubjectName(@Param("subject") String subject, @Param("limit") int limit);
    
    // 비동기 환경에서 db 연결 종료되어 접근 못 하는 문제 방지
    @Query("SELECT q FROM Question q " +
            "JOIN FETCH q.subject " +
            "LEFT JOIN FETCH q.keywords " +
            "WHERE q.id = :id")
    Optional<Question> findWithSubjectAndKeywordsById(@Param("id") Long id);
}
