package com.project.backend.controller;

import com.project.backend.dto.request.InterviewCompleteRequest;
import com.project.backend.dto.request.InterviewStartRequest;
import com.project.backend.dto.response.AnswerEvaluationResponse;
import com.project.backend.dto.response.InterviewDetailResponse;
import com.project.backend.dto.response.InterviewStartResponse;
import com.project.backend.dto.response.InterviewSummaryResponse;
import com.project.backend.service.InterviewService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

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
    
    @PostMapping(value = "/{interviewId}/answers",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnswerEvaluationResponse> evaluateAnswer(
            @RequestParam("userId") Long userId,
            @PathVariable String interviewId,
            @RequestParam("questionId") Long questionId,
            @RequestPart("audio") MultipartFile audioFile) {
        
        AnswerEvaluationResponse response = interviewService.evaluateAnswer(
                userId, interviewId, questionId, audioFile
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{interviewId}/complete")
    public ResponseEntity<InterviewSummaryResponse> completeInterview(
            @PathVariable String interviewId,
            @RequestBody InterviewCompleteRequest req) {
        return ResponseEntity.ok(interviewService.completeInterview(
                interviewId, req.answers()));
    }
    
    @GetMapping("/{interviewId}/results")
    public ResponseEntity<InterviewDetailResponse> getInterviewResult(
            @PathVariable String interviewId) {
        return ResponseEntity.ok(interviewService.getInterviewResult(interviewId));
    }
}