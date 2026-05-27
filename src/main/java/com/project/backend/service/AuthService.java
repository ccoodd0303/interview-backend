package com.project.backend.service;

import com.project.backend.domain.User;
import com.project.backend.dto.request.LoginRequest;
import com.project.backend.dto.request.RegisterRequest;
import com.project.backend.dto.response.LoginResponse;
import com.project.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt; // BCrypt 라이브러리 임포트
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        
        String hashedPassword = BCrypt.hashpw(request.password(), BCrypt.gensalt());
        
        User newUser = User.builder()
                .email(request.email())
                .password(hashedPassword)
                .nickname(request.nickname())
                .build();
        userRepository.save(newUser);
    }
    
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));
        
        if (!BCrypt.checkpw(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        
        return new LoginResponse(user.getId(), user.getNickname());
    }
}
