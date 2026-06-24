package com.project.backend.repository;

import com.project.backend.domain.AnswerKeywordResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerKeywordResultRepository extends JpaRepository<AnswerKeywordResult, Long> {
    List<AnswerKeywordResult> findByAnswerId(Long answerId);
}
