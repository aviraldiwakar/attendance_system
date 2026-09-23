package com.attendance.teacherserver;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;

@Service
public class NetworkDiscoveryService {

    private JmDNS jmdns;

    @EventListener(ApplicationReadyEvent.class)
    public void startBroadcasting() {
        try {
            // Retrieve the local IP address of the teacher's machine
            InetAddress localHost = InetAddress.getLocalHost();

            // Initialize JmDNS on this specific IP
            jmdns = JmDNS.create(localHost);

            // Define the network service: Type, Name, Port, and Description
            ServiceInfo serviceInfo = ServiceInfo.create(
                    "_attendance._tcp.local.",
                    "current Class",
                    8080,
                    "AI Attendance Server"
            );

            // Register and begin broadcasting
            jmdns.registerService(serviceInfo);
            System.out.println("Network broadcast started on: " + localHost.getHostAddress());
            System.out.println("Broadcasting as: " + serviceInfo.getName());

        } catch (IOException e) {
            System.err.println("Error starting network discovery: " + e.getMessage());
        }
    }

    // Ensure the broadcast stops cleanly when the server is shut down
    @PreDestroy
    public void stopBroadcasting() {
        if (jmdns != null) {
            jmdns.unregisterAllServices();
            try {
                jmdns.close();
                System.out.println("Network broadcast stopped.");
            } catch (IOException e) {
                System.err.println("Error closing JmDNS: " + e.getMessage());
            }
        }
    }
}