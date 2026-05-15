package com.project.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "question", indexes = @Index(name = "idx_category", columnList = "category"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String category;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> targetKeywords;
    

    @Builder
    private Question(String category, String questionText, List<String> targetKeywords) {
        this.category = category;
        this.questionText = questionText;
        this.targetKeywords = targetKeywords;
    }
}
