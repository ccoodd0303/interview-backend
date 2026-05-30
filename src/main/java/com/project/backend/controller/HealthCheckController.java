package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthCheckController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Runtime runtime = Runtime.getRuntime();
        
        long maxMemory = runtime.maxMemory();      // JVM이 사용할 수 있는 최대 메모리 (-Xmx)
        long totalMemory = runtime.totalMemory();  // JVM이 현재 OS로부터 할당받은 총 메모리
        long freeMemory = runtime.freeMemory();    // 할당받은 메모리 중 여유 공간
        long usedMemory = totalMemory - freeMemory; // 실제로 현재 사용 중인 메모리

        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("maxMemoryMb", maxMemory / 1024 / 1024);
        status.put("totalMemoryMb", totalMemory / 1024 / 1024);
        status.put("usedMemoryMb", usedMemory / 1024 / 1024);
        status.put("freeMemoryMb", freeMemory / 1024 / 1024);
        
        return ResponseEntity.ok(status);
    }
}