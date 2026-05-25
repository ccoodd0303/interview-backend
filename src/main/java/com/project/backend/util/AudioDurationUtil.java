package com.project.backend.util;

import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

// ffprobe(FFmpeg 멀티미디어 분석 도구)를 실행하여 오디오의 duration 값을 읽어 재생 길이를 추출
@Slf4j
public class AudioDurationUtil {
    
    // ffprobe 비정상 응답 시 무한 대기 방지
    private static final long TIMEOUT_SECONDS = 10;
    
    public static int extractDuration(Path audioFilePath) {
        if (audioFilePath == null) {
            return 0;
        }
        
        try {
            // ffprobe로 오디오 길이 조회
            Process process = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    audioFilePath.toAbsolutePath().toString()
            ).start();
            
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return 0;
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String result = reader.readLine();
                
                // 초 단위로 길이 리턴
                return result == null ? 0 : (int) Double.parseDouble(result);
            }
            
        } catch (Exception e) {
            log.warn("duration extract failed - path={}", audioFilePath, e);
            return 0;
        }
    }
}