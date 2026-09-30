package com.attendance.teacherserver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    @PostMapping("/attendance")
    public ResponseEntity<String> markAttendance(@RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("studentName");
            String enrollmentId = (String) payload.get("enrollmentId");
            long timestampMillis = Long.parseLong(payload.get("timestamp").toString());

            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneId.systemDefault());
            String timeString = dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String dateString = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            String fileName = "Attendance_" + dateString + ".csv";
            File file = new File(fileName);

            // 1. DUPLICATE CHECK: Read the file to see if the ID is already there
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        // Check if the line starts with this specific student's ID
                        if (line.startsWith(enrollmentId + ",")) {
                            System.out.println("⚠️ ALREADY MARKED: " + name + " (" + enrollmentId + ")");
                            // Return a 409 Conflict status so the Android app knows it's a duplicate
                            return ResponseEntity.status(409).body("Already Marked");
                        }
                    }
                }
            }

            boolean isNewFile = !file.exists();
            FileWriter fw = new FileWriter(file, true);
            PrintWriter pw = new PrintWriter(fw);

            if (isNewFile) {
                pw.println("Enrollment ID,Student Name,Time");
            }

            pw.println(enrollmentId + "," + name + "," + timeString);
            pw.flush();
            pw.close();

            System.out.println("✅ SAVED TO CSV: " + name + " (" + enrollmentId + ") at " + timeString);
            return ResponseEntity.ok("Success");

        } catch (Exception e) {
            System.err.println("❌ Error saving to CSV: " + e.getMessage());
            return ResponseEntity.status(500).body("Server Error");
        }
    }
}