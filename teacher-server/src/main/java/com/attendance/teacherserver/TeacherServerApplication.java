package com.attendance.teacherserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TeacherServerApplication {
    public static void main(String[] args) {
        // Force Java to use IPv4 to prevent the setsockopt multicast error
        System.setProperty("java.net.preferIPv4Stack", "true");

        SpringApplication.run(TeacherServerApplication.class, args);
    }
}