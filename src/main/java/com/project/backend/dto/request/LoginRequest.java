package com.project.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 로그인 요청 시 인증을 위한 이메일과 비밀번호를 전달하는 DTO
public record LoginRequest(
        @Email(message = "올바른 이메일 형식을 입력해주세요.")
        @NotBlank(message = "이메일을 입력해주세요.")
        String email,
        
        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {}
