package com.project.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 회원가입 요청 시 이메일, 비밀번호, 닉네임을 전달하는 DTO
public record RegisterRequest(
        @Email(message = "이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        @Size(max = 100, message = "이메일은 최대 100자입니다.")
        String email,
        
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*\\W).{4,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자 조합 4~20자여야 합니다.")
        String password,
        
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 10, message = "닉네임은 최대 10자입니다.")
        String nickname
) {}
