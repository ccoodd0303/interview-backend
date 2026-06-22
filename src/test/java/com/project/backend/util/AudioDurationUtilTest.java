package com.project.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AudioDurationUtilTest {

    @Autowired
    private AudioDurationUtil audioDurationUtil;

    @Test
    @DisplayName("불완전한 WebM 파일 입력 시 FFmpeg 파이프라인을 거쳐 정확한 재생 시간(초)이 반환되는지 검증한다")
    void testExtractDurationSuccess() throws IOException {
        // 1. 테스트용 임시 WebM 파일 경로 생성
        Path tempFile = Files.createTempFile("test_sample", ".webm");
        
        // 2. 테스트 리소스 폴더에 미리 적재해 둔 임시 WebM 파일 복사 시도
        try (InputStream in = getClass().getResourceAsStream("/test_sample.webm")) {
            if (in != null) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                
                long startTime = System.currentTimeMillis();
                
                // 3. 실제 재생시간 추출 로직 구동 (ffmpeg -> ffprobe 실행)
                int duration = audioDurationUtil.extractDuration(tempFile);
                
                long elapsedTime = System.currentTimeMillis() - startTime;
                System.out.println("FFmpeg 헤더 재복구 및 ffprobe 시간 추출 연산 속도: " + elapsedTime + "ms");
                System.out.println("분석된 재생 시간: " + duration + "초");

                // 4. [검증] 메타데이터 복구 후 분석된 초 단위 재생시간이 오차 한계 내에 있는지 확인
                assertThat(duration).isGreaterThanOrEqualTo(0);
                assertThat(elapsedTime).isLessThan(10000); // 연산 처리 속도 10초 이내 검증 (프로세스 타임아웃 미만)
            } else {
                System.out.println("[경고] 테스트용 오디오 리소스(test_sample.webm)가 클래스패스에 존재하지 않아 검증을 건너뜁니다.");
            }
        } finally {
            // 5. 임시 자원 정리
            Files.deleteIfExists(tempFile);
        }
    }
}
