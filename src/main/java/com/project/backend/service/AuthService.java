package com.project.backend.service;

import com.project.backend.domain.User;
import com.project.backend.dto.request.LoginRequest;
import com.project.backend.dto.request.RegisterRequest;
import com.project.backend.dto.response.LoginResponse;
import com.project.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Duplicate email");
        }
        
        User newUser = User.builder()
                .email(request.email())
                .password(request.password())
                .nickname(request.nickname())
                .build();
        userRepository.save(newUser);
    }
    
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Unauthorized"));
        
        if (!request.password().equals(user.getPassword())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        
        return new LoginResponse(user.getId(), user.getNickname());
    }
}