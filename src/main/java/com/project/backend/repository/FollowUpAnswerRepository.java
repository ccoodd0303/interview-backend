package com.project.backend.repository;

import com.project.backend.domain.FollowUpAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowUpAnswerRepository extends JpaRepository<FollowUpAnswer, Long> {
    Optional<FollowUpAnswer> findFirstByAnswerIdAndKeywordIdOrderByIdDesc(Long answerId, Long keywordId);
}
