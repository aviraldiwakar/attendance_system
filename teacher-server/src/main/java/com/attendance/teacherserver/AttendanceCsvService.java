package com.attendance.teacherserver;

import com.opencsv.CSVWriter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AttendanceCsvService {

    // The file will be created in your project's root directory
    private static final String FILE_PATH = "attendance_record.csv";

    public void logAttendance(AttendanceRequest request) {
        File file = new File(FILE_PATH);
        boolean isNewFile = !file.exists();

        // FileWriter with 'true' enables append mode
        try (CSVWriter writer = new CSVWriter(new FileWriter(FILE_PATH, true))) {

            // 1. Write column headers if the file was just created
            if (isNewFile) {
                writer.writeNext(new String[]{"Name", "Enrollment Number", "Timestamp", "Liveness Verified"});
            }

            // 2. Format the current time cleanly
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 3. Write the student's data as a new row
            String[] record = {
                    request.getStudentName(),
                    request.getEnrollmentNumber(),
                    timestamp,
                    "Yes"
            };
            writer.writeNext(record);

            System.out.println("Successfully saved to CSV: " + request.getStudentName());

        } catch (IOException e) {
            System.err.println("Error writing to CSV: " + e.getMessage());
        }
    }
}