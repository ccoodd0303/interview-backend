package com.project.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

// ffmpeg/ffprobe 활용 오디오 길이 추출 유틸리티
@Slf4j
@Component
public class AudioDurationUtil {
    
    // 프로세스 실행 타임아웃
    private static final long TIMEOUT_SECONDS = 10;
    
    public int extractDuration(Path audioFilePath) {
        Path fixedPath = null;
        try {
            // 파일 크기 검증
            if (audioFilePath == null || !Files.exists(audioFilePath)) {
                log.warn("오디오 파일이 존재하지 않거나 경로가 null입니다. (경로: {})", audioFilePath);
                return 0;
            }
            
            long size = Files.size(audioFilePath);
            if (size < 5120) { // 5KB 미만
                log.warn("오디오 파일 크기가 너무 작습니다. (크기: {} bytes, 경로: {})", size, audioFilePath);
                return 0;
            }
            
            // 임시 복구 파일 경로 생성
            String originalName = audioFilePath.getFileName().toString();
            String baseName = originalName;
            if (originalName.contains(".")) {
                baseName = originalName.substring(0, originalName.lastIndexOf("."));
            }
            String fixedFilename = baseName + "_fixed.webm";
            fixedPath = audioFilePath.resolveSibling(fixedFilename);
            
            // ffmpeg 실행하여 헤더 복구
            Process ffmpegProcess = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", audioFilePath.toAbsolutePath().toString(),
                    "-c", "copy",
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
            
            // ffprobe 실행하여 오디오 길이 추출
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
            
            // ffprobe 결과 읽기 및 변환
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ffprobeProcess.getInputStream()))) {
                
                String result = reader.readLine();
                
                // N/A 방어 처리
                if (result == null || result.isBlank() || "N/A".equalsIgnoreCase(result.trim())) {
                    return 0;
                }
                
                // 반올림 후 반환
                double parsedDuration = Double.parseDouble(result.trim());
                return (int) Math.round(parsedDuration);
            }
            
        } catch (Exception e) {
            log.error("오디오 재생 길이 추출 실패: {}", audioFilePath, e);
            return 0;
        } finally {
            // 임시 파일 삭제
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