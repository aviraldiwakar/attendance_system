package com.attendance.teacherserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    // Inject the CSV service we just created
    @Autowired
    private AttendanceCsvService csvService;

    @PostMapping("/mark-attendance")
    public ResponseEntity<String> markAttendance(@RequestBody AttendanceRequest request) {
        // 1. Verify the AI check passed on the mobile device
        if (!request.isLivenessVerified()) {
            return ResponseEntity.badRequest().body("Spoofing detected or liveness check failed.");
        }

        // 2. Log the successful attendance into the CSV file
        csvService.logAttendance(request);

        return ResponseEntity.ok("Attendance successfully recorded and saved to file.");
    }
}