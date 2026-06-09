package com.project.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.dto.request.InterviewStartRequest;
import com.project.backend.dto.response.InterviewDetailResponse;
import com.project.backend.dto.response.InterviewStartResponse;
import com.project.backend.service.InterviewService;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {
    
    private final InterviewService interviewService;
    
    @PostMapping
    public ResponseEntity<InterviewStartResponse> startInterview(
            @RequestBody InterviewStartRequest req) {
        return ResponseEntity.ok(interviewService.startInterview(
                req.userId(), req.subject()));
    }
    
    // 음성 답변 제출
    @PostMapping(value = "/{interviewId}/answers",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> evaluateAnswer(
            @PathVariable String interviewId,
            @RequestParam("userId") String userId,
            @RequestParam("questionId") String questionId,
            @RequestPart("audio") MultipartFile audioFile) {
        
        Path tempPath = null;
        try {
            if (audioFile == null || audioFile.isEmpty()) {
                throw new IllegalArgumentException("음성 파일이 비어있거나 전송되지 않았습니다.");
            }
            
            // 오디오 임시 파일 생성 후 서버에 로컬 파일로 저장
            String filename = audioFile.getOriginalFilename();
            String suffix = (filename != null && filename.contains(".")) ?
                    filename.substring(filename.lastIndexOf(".")) : ".tmp";
            tempPath = Files.createTempFile("interview_", suffix);
            audioFile.transferTo(tempPath.toFile());
            
            interviewService.evaluateAnswerAsync(
                    Long.parseLong(userId), interviewId,
                    Long.parseLong(questionId),
                    tempPath
            );
        } catch (Exception e) {
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException ignored) {
                }
            }
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            log.error("파일 전송/읽기 실패 - interviewId: {}, userId: {}, questionId: {}",
                    interviewId, userId, questionId, e);
            throw new IllegalArgumentException("서버에서 음성 파일을 읽는 데 실패했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok().build();
    }
    
    // 면접 완료 요청
    @PostMapping("/{interviewId}/complete")
    public ResponseEntity<InterviewDetailResponse> completeInterview(
            @PathVariable String interviewId) {
        
        InterviewDetailResponse response =
                interviewService.completeInterview(interviewId);
        
        return ResponseEntity.ok(response);
    }
    
    
    // 면접 결과 조회
    @GetMapping("/{interviewId}/results")
    public ResponseEntity<InterviewDetailResponse> getInterviewResult(
            @PathVariable String interviewId) {
        return ResponseEntity.ok(interviewService.getInterviewResult(interviewId));
    }
}
