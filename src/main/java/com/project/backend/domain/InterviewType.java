package com.project.backend.domain;

public enum InterviewType {
    GENERAL,
    ADVANCED;

    public static InterviewType from(String value) {
        if (value == null || value.isBlank()) {
            return GENERAL;
        }
        return InterviewType.valueOf(value.trim().toUpperCase());
    }
}
