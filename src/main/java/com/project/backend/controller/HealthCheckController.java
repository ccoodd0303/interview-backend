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
        
        long maxMemory = runtime.maxMemory();      
        long totalMemory = runtime.totalMemory();  
        long freeMemory = runtime.freeMemory();   
        long usedMemory = totalMemory - freeMemory;

        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("maxMemoryMb", maxMemory / 1024 / 1024);
        status.put("totalMemoryMb", totalMemory / 1024 / 1024);
        status.put("usedMemoryMb", usedMemory / 1024 / 1024);
        status.put("freeMemoryMb", freeMemory / 1024 / 1024);
        
        return ResponseEntity.ok(status);
    }
}