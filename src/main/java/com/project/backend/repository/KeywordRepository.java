package com.project.backend.repository;

import com.project.backend.domain.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    List<Keyword> findByQuestionIdOrderByImportanceDescIdAsc(Long questionId);
}
