package com.project.backend.util;

import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AudioDurationUtil {
    
    // ffprobe 비정상 응답 시 무한 대기 방지
    private static final long TIMEOUT_SECONDS = 10;
    
    public static int extractDuration(Path audioFilePath) {
        if (audioFilePath == null) {
            return 0;
        }
        
        try {
            // ffprobe로 오디오 길이(초) 조회
            Process process = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    audioFilePath.toAbsolutePath().toString()
            ).start();
            
            // 지정 시간 내 종료되지 않으면 프로세스 강제 종료
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return 0;
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String result = reader.readLine();
                
                // ffprobe 결과를 초 단위 정수로 변환
                return result == null ? 0 : (int) Double.parseDouble(result);
            }
            
        } catch (Exception e) {
            log.warn("duration extract failed - path={}", audioFilePath, e);
            return 0;
        }
    }
}