package com.project.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

// ffmpeg 및 ffprobe를 사용한 오디오 재생 시간(길이) 추출 유틸리티
@Slf4j
@Component
public class AudioDurationUtil {
    
    // 프로세스 실행 타임아웃
    private static final long TIMEOUT_SECONDS = 10;
    
    public int extractDuration(Path audioFilePath) {
        Path fixedPath = null;
        try {
            // 비어있거나 손상된 파일인지 크기 확인
            if (audioFilePath == null || !Files.exists(audioFilePath)) {
                log.warn("오디오 파일이 존재하지 않거나 경로가 null입니다. (경로: {})", audioFilePath);
                return 0;
            }
            
            
            // 손상된 헤더 복구를 위한 임시 파일 경로 설정
            String originalName = audioFilePath.getFileName().toString();
            String baseName = originalName;
            if (originalName.contains(".")) {
                baseName = originalName.substring(0, originalName.lastIndexOf("."));
            }
            String fixedFilename = baseName + "_fixed.webm";
            fixedPath = audioFilePath.resolveSibling(fixedFilename);
            
            // ffmpeg로 파일 헤더를 복구하고 opus 코덱으로 재인코딩
            Process ffmpegProcess = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", audioFilePath.toAbsolutePath().toString(),
                    "-c:a", "libopus",
                    fixedPath.toAbsolutePath().toString()
            ).start();
            
            // ffmpeg 대기 및 예외 처리
            if (!ffmpegProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                ffmpegProcess.destroyForcibly();
                throw new IOException("FFmpeg metadata reconstruction timed out");
            }
            
            if (ffmpegProcess.exitValue() != 0) {
                throw new IOException("FFmpeg process failed with exit value: " + ffmpegProcess.exitValue());
            }
            
            // ffprobe로 오디오 재생 시간 조회
            Process ffprobeProcess = new ProcessBuilder(
                    "ffprobe", "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    fixedPath.toAbsolutePath().toString()
            ).start();
            
            // ffprobe 대기 및 예외 처리
            if (!ffprobeProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                ffprobeProcess.destroyForcibly();
                throw new IOException("FFprobe duration extraction timed out");
            }
            
            // ffprobe 출력 결과 파싱 및 초 단위 변환
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ffprobeProcess.getInputStream()))) {
                
                String result = reader.readLine();
                
                // 재생 시간이 정상 출력되지 않는 경우(N/A) 예외 처리
                if (result == null || result.isBlank() || "N/A".equalsIgnoreCase(result.trim())) {
                    return 0;
                }
                
                // 소수점 이하 반올림하여 반환
                double parsedDuration = Double.parseDouble(result.trim());
                return (int) Math.round(parsedDuration);
            }
            
        } catch (Exception e) {
            log.error("오디오 재생 길이 추출 실패: {}", audioFilePath, e);
            return 0;
        } finally {
            // 작업 완료 후 임시 파일 삭제
            if (fixedPath != null) {
                try {
                    Files.deleteIfExists(fixedPath);
                } catch (IOException e) {
                    log.warn("임시 복구 파일 삭제 실패: {}", fixedPath, e);
                }
            }
        }
    }
}