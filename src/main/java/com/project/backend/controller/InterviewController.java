package com.project.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.dto.request.InterviewStartRequest;
import com.project.backend.dto.request.InterviewCompleteRequest;
import com.project.backend.dto.request.FollowUpAnswerRequest;
import com.project.backend.dto.response.AnswerSubmitResponse;
import com.project.backend.dto.response.FollowUpAnswerResponse;
import com.project.backend.dto.response.InterviewDetailResponse;
import com.project.backend.dto.response.InterviewStartResponse;
import com.project.backend.domain.InterviewType;
import com.project.backend.service.InterviewService;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

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
                req.userId(), req.subject(), req.mode(),
                InterviewType.from(req.type()), req.questionCount()));
    }
    
    // 음성 답변 제출
    @PostMapping(value = "/{interviewId}/answers",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> evaluateAnswer(
            @PathVariable String interviewId,
            @RequestParam("userId") String userId,
            @RequestParam("questionId") String questionId,
            @RequestParam(value = "mode", required = false) String mode,
            @RequestParam(value = "type", required = false) String type,
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
            
            InterviewType interviewType = InterviewType.from(type);
            if (type == null || type.isBlank()) {
                interviewService.evaluateAnswerAsync(
                        Long.parseLong(userId), interviewId,
                        Long.parseLong(questionId),
                        tempPath
                );
                return ResponseEntity.ok().build();
            }

            AnswerSubmitResponse response = interviewService.evaluateAnswer(
                    Long.parseLong(userId), interviewId,
                    Long.parseLong(questionId),
                    tempPath, mode, interviewType);
            return ResponseEntity.ok(response);
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
    }
    
    // 면접 완료 요청
    @PostMapping("/{interviewId}/complete")
    public ResponseEntity<InterviewDetailResponse> completeInterview(
            @PathVariable String interviewId,
            @RequestBody(required = false) InterviewCompleteRequest request) {
        
        InterviewDetailResponse response =
                request == null
                        ? interviewService.completeInterview(interviewId)
                        : interviewService.completeInterview(interviewId, request);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{interviewId}/follow-up-answers")
    public ResponseEntity<FollowUpAnswerResponse> submitFollowUpAnswer(
            @PathVariable String interviewId,
            @RequestBody FollowUpAnswerRequest request) {
        return ResponseEntity.ok(interviewService.saveFollowUpAnswer(interviewId, request));
    }

    @PostMapping(value = "/{interviewId}/follow-up-answers/audio",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FollowUpAnswerResponse> submitFollowUpAnswerAudio(
            @PathVariable String interviewId,
            @RequestParam("answerId") Long answerId,
            @RequestParam("keywordId") Long keywordId,
            @RequestParam("followUpQuestion") String followUpQuestion,
            @RequestPart("audio") MultipartFile audioFile) {

        Path tempPath = null;
        try {
            if (audioFile == null || audioFile.isEmpty()) {
                throw new IllegalArgumentException("꼬리답변 음성 파일이 비어있거나 전송되지 않았습니다.");
            }

            String filename = audioFile.getOriginalFilename();
            String suffix = (filename != null && filename.contains(".")) ?
                    filename.substring(filename.lastIndexOf(".")) : ".tmp";
            tempPath = Files.createTempFile("follow_up_", suffix);
            audioFile.transferTo(tempPath.toFile());

            return ResponseEntity.ok(interviewService.saveFollowUpAnswerAudio(
                    interviewId, answerId, keywordId, followUpQuestion, tempPath));
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
            log.error("꼬리답변 음성 파일 처리 실패 - interviewId: {}, answerId: {}, keywordId: {}",
                    interviewId, answerId, keywordId, e);
            throw new IllegalArgumentException("서버에서 꼬리답변 음성 파일을 읽는 데 실패했습니다: " + e.getMessage());
        }
    }
    
    
    // 면접 결과 조회
    @GetMapping("/{interviewId}/results")
    public ResponseEntity<InterviewDetailResponse> getInterviewResult(
            @PathVariable String interviewId) {
        return ResponseEntity.ok(interviewService.getInterviewResult(interviewId));
    }
}
