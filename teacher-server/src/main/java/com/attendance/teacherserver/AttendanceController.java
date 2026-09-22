package com.attendance.teacherserver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    @PostMapping("/mark-attendance")
    public ResponseEntity<String> markAttendance(@RequestBody com.attendance.teacherserver.AttendanceRequest request) {
        // 1. Verify the AI check passed on the mobile device
        if (!request.isLivenessVerified()) {
            return ResponseEntity.badRequest().body("Spoofing detected or liveness check failed.");
        }

        // 2. Log the successful attendance (Later, we will write this to CSV)
        System.out.println("Attendance marked for: " + request.getStudentName() +
                " (" + request.getEnrollmentNumber() + ") at " + LocalDateTime.now());

        return ResponseEntity.ok("Attendance successfully recorded.");
    }
}