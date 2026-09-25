package com.attendance.teacherserver;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Service
public class NetworkDiscoveryService {

    // Keep track of all broadcast instances so we can close them cleanly later
    private final List<JmDNS> jmdnsInstances = new ArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void startBroadcasting() {
        try {
            // 1. Get every network connection on the laptop (Wi-Fi, LAN, Hotspot)
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // 2. Ignore inactive networks and the internal loopback (127.0.0.1)
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    // 3. Only bind to standard IPv4 addresses
                    if (addr instanceof Inet4Address) {
                        try {
                            JmDNS jmdns = JmDNS.create(addr);
                            ServiceInfo serviceInfo = ServiceInfo.create(
                                    "_attendance._tcp.local.",
                                    "Current Class",
                                    8080,
                                    "AI Attendance Server"
                            );
                            jmdns.registerService(serviceInfo);
                            jmdnsInstances.add(jmdns);

                            System.out.println("Broadcasting on: " + networkInterface.getDisplayName() + " | IP: " + addr.getHostAddress());
                        } catch (IOException e) {
                            System.err.println("Skipped IP " + addr.getHostAddress() + " - " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error enumerating network interfaces: " + e.getMessage());
        }
    }

    @PreDestroy
    public void stopBroadcasting() {
        for (JmDNS jmdns : jmdnsInstances) {
            if (jmdns != null) {
                jmdns.unregisterAllServices();
                try {
                    jmdns.close();
                } catch (IOException e) {
                    System.err.println("Error closing JmDNS: " + e.getMessage());
                }
            }
        }
        System.out.println("All network broadcasts stopped.");
    }
}