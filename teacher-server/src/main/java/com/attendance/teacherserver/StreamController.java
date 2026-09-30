package com.attendance.teacherserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    @Autowired
    private NetworkDiscoveryService networkService;

    @PostMapping("/start")
    public ResponseEntity<?> startStream(@RequestBody Map<String, Object> payload) {
        String className = (String) payload.get("className");
        int timeout = Integer.parseInt(payload.get("timeout").toString());

        boolean success = networkService.startBroadcasting(className, timeout);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Stream started successfully"));
        }
        return ResponseEntity.status(500).body(Map.of("error", "Failed to start stream"));
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopStream() {
        networkService.stopBroadcasting();
        return ResponseEntity.ok(Map.of("message", "Stream stopped"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("active", networkService.isBroadcasting());
        status.put("className", networkService.getCurrentClassName());
        return ResponseEntity.ok(status);
    }
}