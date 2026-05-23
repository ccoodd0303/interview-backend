package com.project.backend.repository;

import com.project.backend.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findBySubjectName(String subject);
    
    @Query("SELECT DISTINCT q.subject.name FROM Question q")
    List<String> findDistinctSubjects();
}
