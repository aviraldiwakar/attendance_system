package com.attendance.teacherserver;

public class AttendanceRequest {
    private String studentName;
    private String enrollmentNumber;
    private boolean livenessVerified;

    // Getters and Setters
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getEnrollmentNumber() { return enrollmentNumber; }
    public void setEnrollmentNumber(String enrollmentNumber) { this.enrollmentNumber = enrollmentNumber; }

    public boolean isLivenessVerified() { return livenessVerified; }
    public void setLivenessVerified(boolean livenessVerified) { this.livenessVerified = livenessVerified; }
}