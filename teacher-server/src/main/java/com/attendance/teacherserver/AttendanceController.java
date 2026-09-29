package com.attendance.teacherserver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    @PostMapping("/attendance")
    public ResponseEntity<String> markAttendance(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("studentName");
        String enrollmentId = (String) payload.get("enrollmentId");

        System.out.println("✅ ATTENDANCE MARKED: " + name + " (" + enrollmentId + ")");

        return ResponseEntity.ok("Success");
    }
}