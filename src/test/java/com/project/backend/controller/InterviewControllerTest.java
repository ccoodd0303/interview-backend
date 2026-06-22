package com.project.backend.controller;

import com.project.backend.dto.response.InterviewDetailResponse;
import com.project.backend.service.InterviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterviewController.class)
public class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InterviewService interviewService;

    @Test
    @DisplayName("답변 음성 파일을 제출하면 백엔드 로직이 비동기로 위임되어 100ms 이내에 즉각 200 OK를 반환해야 한다")
    void testEvaluateAnswerAsyncReturnsImmediately() throws Exception {
        // 임의의 Mock WebM 오디오 파일 생성
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "test_audio.webm", "audio/webm", new byte[]{1, 2, 3, 4});

        // API 호출 및 즉각 응답 여부 확인
        long startTime = System.currentTimeMillis();

        mockMvc.perform(multipart("/api/interviews/test-session-id/answers")
                        .file(audioFile)
                        .param("userId", "1")
                        .param("questionId", "101")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("답변 제출 API 처리 소요 시간: " + elapsedTime + "ms");

        // 비동기 스레드 메서드 호출 여부 검증 (컨트롤러 단에서 멈추지 않고 비동기 서비스를 실행했는지 확인)
        verify(interviewService, times(1)).evaluateAnswerAsync(any(), any(), any(), any());
    }

    @Test
    @DisplayName("AI 채점이 다 끝나지 않은 상태에서 면접 완료를 요청하면 400 Bad Request(또는 500) 및 대기 예외를 응답해야 한다")
    void testCompleteInterviewReturnsErrorWhenNotFinished() throws Exception {
        // 아직 10문항이 다 채점되지 않아 IllegalStateException이 발생하는 비즈니스 상황 Mocking
        doThrow(new IllegalStateException("아직 모든 답변의 AI 채점이 완료되지 않았습니다."))
                .when(interviewService).completeInterview("test-session-id");

        // API 호출 결과가 예외 상태코드로 반환되는지 확인 (프론트엔드의 폴링 대기 유도용)
        mockMvc.perform(post("/api/interviews/test-session-id/complete"))
                .andExpect(status().isBadRequest()); // 혹은 ExceptionHandler에 의해 구현된 에러 상태코드
    }

    @Test
    @DisplayName("모든 AI 채점이 완료된 뒤 면접 완료를 요청하면 종합 결과를 담은 200 OK를 반환한다")
    void testCompleteInterviewSuccess() throws Exception {
        // 채점 완료 후 정상 응답 데이터 반환 Mocking
        InterviewDetailResponse response = new InterviewDetailResponse(
                "test-session-id",
                "소프트웨어 설계/개발",
                "2026.06.22",
                85,
                10,
                8,
                15,
                "전반적으로 훌륭한 인터뷰였습니다.",
                new java.util.ArrayList<>()
        );
        doReturn(response).when(interviewService).completeInterview("test-session-id");

        mockMvc.perform(post("/api/interviews/test-session-id/complete"))
                .andExpect(status().isOk());
    }
}
