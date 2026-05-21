package com.project.backend.util;

// 필수 패키지 임포트
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AudioDurationUtil {
    
    private static final long TIMEOUT_SECONDS = 10;
    
    // 파라미터를 Path 객체로만 받아 읽기 전용으로 길이를 추출합니다.
    public static int extractDuration(Path audioFilePath) {
        if (audioFilePath == null) {
            return 0;
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    audioFilePath.toAbsolutePath().toString()
            );
            
            // 외부 프로세스의 에러 스트림을 표준 출력으로 병합하여 교착 상태 방지
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            String result;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                result = reader.readLine();
            }
            
            // 무한 대기(Hanging) 방지를 위한 10초 타임아웃 가드
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("ffprobe timeout - path={}", audioFilePath);
                return 0;
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0 || result == null || result.isBlank()) {
                log.warn("ffprobe failed - path={}, result={}", audioFilePath, result);
                return 0;
            }
            
            // 소수점으로 반환된 초 데이터를 정수(버림/캐스팅)로 변환
            return (int) Double.parseDouble(result);
            
        } catch (Exception e) {
            log.warn("duration extract failed - path={}", audioFilePath, e);
            return 0;
        }
    }
}