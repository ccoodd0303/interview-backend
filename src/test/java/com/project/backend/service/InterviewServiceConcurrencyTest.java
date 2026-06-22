package com.project.backend.service;

import com.project.backend.domain.User;
import com.project.backend.domain.SessionStatus;
import com.project.backend.domain.InterviewSession;
import com.project.backend.repository.UserRepository;
import com.project.backend.repository.InterviewSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class InterviewServiceConcurrencyTest {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Long testUserId;
    private final String testSubject = "소프트웨어 설계/개발";

    @BeforeEach
    void setUp() {
        // 외래 키 순서에 따른 테이블 데이터 초기화
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM question_keywords");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM subjects");

        // 테스트용 과목 데이터 삽입
        jdbcTemplate.update("INSERT INTO subjects (id, name) VALUES (1, '소프트웨어 설계/개발')");

        // 테스트용 질문 10개 데이터 삽입 (출제 요구조건 충족)
        for (int i = 1; i <= 10; i++) {
            jdbcTemplate.update(
                "INSERT INTO questions (id, subject_id, title, ideal_answer, difficulty) VALUES (?, 1, ?, '모범답안입니다.', 3)",
                (long) i, "테스트 질문 " + i
            );
        }

        // 테스트용 사용자 인스턴스 생성 및 영속화
        User user = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스터")
                .build();
        User savedUser = userRepository.save(user);
        testUserId = savedUser.getId();
    }

    @Test
    @DisplayName("동시에 10개의 면접 시작 요청이 유입될 때, 최종 세션은 단 1개만 유효하게 남아야 한다")
    void testConcurrentStartInterview() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        
        // 10개 스레드가 동시에 출발선에서 대기하도록 제어하는 Latch
        CountDownLatch readyLatch = new CountDownLatch(1);
        // 10개 스레드의 수행 완료를 대기하는 Latch
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.await(); // 모든 스레드가 준비될 때까지 대기
                    interviewService.startInterview(testUserId, testSubject);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet(); // 예외가 발생하거나 거절된 요청 기록
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 10개 스레드에 동시 호출 신호 전송 (레이스 컨디션 유도)
        readyLatch.countDown();
        doneLatch.await(); // 모든 스레드가 동작을 마칠 때까지 대기
        executorService.shutdown();

        // 최종 데이터베이스 상태 검증
        List<InterviewSession> activeSessions = sessionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(testUserId, SessionStatus.IN_PROGRESS);

        System.out.println("=== 동시성 테스트 수행 결과 ===");
        System.out.println("동시 요청 시도 스레드 수: " + numberOfThreads);
        System.out.println("API 호출 성공 수 (메서드 정상 반환): " + successCount.get());
        System.out.println("API 호출 실패/차단 수: " + failureCount.get());
        System.out.println("DB에 생성된 최종 활성 세션(IN_PROGRESS) 개수: " + activeSessions.size());

        // [핵심 검증] DB에 생성된 최종 세션은 반드시 1개여야 함
        assertThat(activeSessions).hasSize(1);
    }
}
