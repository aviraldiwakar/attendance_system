# AI-Powered Network Attendance System

A robust, decentralized attendance tracking system built on a dual-node architecture (Java Spring Boot server + Kotlin Android client). It leverages zero-configuration service discovery, biometric liveness detection, and cryptographic hardware locking to create a seamless and tamper-proof attendance flow.

## 🚀 Key Features

* **Zero-Config Discovery (JmDNS):** Android clients dynamically discover the active teacher server on the local network via mDNS broadcasts, eliminating the need for hardcoded IPs or manual port configuration.
* **Biometric Liveness Detection:** Utilizes Android CameraX and Google ML Kit Face Detection. The neural network ensures attendance is only marked by a live human performing a specific action (blinking), effectively preventing photo or video spoofing.
* **Hardware-Level Anti-Cheat (`ANDROID_ID`):** Binds attendance records to the physical device's unalterable 64-bit hardware ID. The server actively blocks device-sharing, preventing students from marking attendance for absent peers.
* **Dynamic Web Dashboard:** A responsive HTML/JS control panel hosted directly by the Spring Boot server. Allows teachers to create custom classes, set automated stream timeouts, and terminate streams manually.
* **Automated CSV Generation:** The Spring Boot backend automatically formats and appends valid incoming HTTP payloads into a daily `.csv` file for easy spreadsheet integration.

## 📂 System Architecture

```text
attendance_system/
 ├── teacher-server/       # Spring Boot Backend (Java 24)
 │    ├── AttendanceController.java    # REST API & CSV Write Logic
 │    ├── StreamController.java        # Web Dashboard API
 │    ├── NetworkDiscoveryService.java # JmDNS Broadcasting Engine
 │    └── static/index.html            # Teacher Web Interface
 │
 └── StudentAttendance/    # Android Client (Kotlin)
      ├── SetupActivity.kt             # Persistent Profile Config
      └── MainActivity.kt              # Dual-Screen UI (Browser -> AI Camera)



🛠️ Technology Stack
Teacher Node (Server):
Java 24
Spring Boot (Web, Tomcat)
JmDNS (Zero-config networking)
HTML5/CSS3/JavaScript (Frontend UI)

Student Node (Client):
Kotlin
Android SDK (Min API 24+)
Google ML Kit (Face Detection)
CameraX API
OkHttp3 (REST communication)


⚙️ Installation & Usage
1. Starting the Teacher Server
Clone the repository and open the teacher-server directory in your IDE (IntelliJ/Eclipse).
Run TeacherServerApplication.java.
Open a web browser and navigate to http://localhost:8080.
Enter your Teacher credentials, create a class session (e.g., "CS101"), and click Start Attendance Stream.

2. Connecting the Student App
Ensure the Android device is connected to the same local network (Wi-Fi or Mobile Hotspot) as the teacher's machine.
Build and install the APK from the StudentAttendance directory.
On first launch, enter the student's Name and Enrollment ID.
The app will scan the network and display active classes. Tap the target class.
Blink at the front-facing camera to verify liveness and transmit the secure payload.


🔒 Security Flow
Liveness Verification: ML Kit confirms leftEyeOpenProbability and rightEyeOpenProbability drop below 0.2 simultaneously.
Data Assembly: App packages Name, Enrollment ID, Timestamp, and ANDROID_ID into JSON.
Transmission: POST request fired to the dynamically resolved IP/Port.
Server Validation: Spring Boot reads the daily CSV. If the Enrollment ID or ANDROID_ID already exists for that session, the request is rejected with 409 Conflict.
Storage: Valid requests are appended to the local CSV.