package com.project.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.dto.request.InterviewStartRequest;
import com.project.backend.dto.response.InterviewDetailResponse;
import com.project.backend.dto.response.InterviewStartResponse;
import com.project.backend.service.InterviewService;

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
                req.userId(), req.category()));
    }
    
    // 음성 답변 제출
    @PostMapping(value = "/{interviewId}/answers",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> evaluateAnswer(
            @PathVariable String interviewId,
            @RequestPart("userId") String userIdStr,
            @RequestPart("questionId") String questionIdStr,
            @RequestPart("audio") MultipartFile audioFile) {
        
        try {
            if (audioFile == null || audioFile.isEmpty()) {
                throw new IllegalArgumentException("음성 파일이 비어있거나 전송되지 않았습니다.");
            }

            Long userId = Long.parseLong(userIdStr);
            Long questionId = Long.parseLong(questionIdStr);
            
            byte[] audioBytes = audioFile.getBytes();
            String filename = audioFile.getOriginalFilename();
            interviewService.evaluateAnswerAsync(
                    userId, interviewId, questionId, audioBytes, filename
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("파일 전송/읽기 실패 - interviewId: {}, userId: {}, questionId: {}", interviewId, userIdStr, questionIdStr, e);
            throw new IllegalArgumentException("서버에서 음성 파일을 읽는 데 실패했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok().build();
    }
    
    // 면접 완료
    @PostMapping("/{interviewId}/complete")
    public ResponseEntity<InterviewDetailResponse> completeInterview(
            @PathVariable String interviewId) {
        return ResponseEntity.ok(interviewService.completeInterview(interviewId));
    }
    
    // 중단한 면접 제거
    @DeleteMapping("/{interviewId}")
    public ResponseEntity<Void> deleteInterview(@PathVariable String interviewId) {
        interviewService.deleteSession(interviewId);
        return ResponseEntity.ok().build();
    }
    
    // 면접 결과 조회
    @GetMapping("/{interviewId}/results")
    public ResponseEntity<InterviewDetailResponse> getInterviewResult(
            @PathVariable String interviewId) {
        return ResponseEntity.ok(interviewService.getInterviewResult(interviewId));
    }
}