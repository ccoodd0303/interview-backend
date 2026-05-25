package com.project.backend.repository;

import com.project.backend.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    @Query("SELECT q FROM Question q WHERE q.subject.name = :subject")
    List<Question> findBySubjectName(@Param("subject") String subject);
    
    @Query("SELECT DISTINCT q.subject.name FROM Question q")
    List<String> findDistinctSubjects();
}
