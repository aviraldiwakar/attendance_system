package com.attendance.teacherserver;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class NetworkDiscoveryService {

    private final List<JmDNS> jmdnsInstances = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> timeoutTask;

    private boolean isBroadcasting = false;
    private String currentClassName = "";

    public synchronized boolean startBroadcasting(String className, int timeoutMinutes) {
        if (isBroadcasting) {
            stopBroadcasting(); // Clean up any existing stream first
        }

        this.currentClassName = className;

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        try {
                            JmDNS jmdns = JmDNS.create(addr);
                            // Inject the dynamic class name into the broadcast
                            ServiceInfo serviceInfo = ServiceInfo.create(
                                    "_attendance._tcp.local.",
                                    className,
                                    8080,
                                    "AI Attendance Server"
                            );
                            jmdns.registerService(serviceInfo);
                            jmdnsInstances.add(jmdns);
                            System.out.println("Started stream for [" + className + "] on IP: " + addr.getHostAddress());
                        } catch (IOException e) {
                            System.err.println("Skipped IP " + addr.getHostAddress() + " - " + e.getMessage());
                        }
                    }
                }
            }

            isBroadcasting = true;

            // Schedule the automatic shutdown
            if (timeoutMinutes > 0) {
                timeoutTask = scheduler.schedule(this::stopBroadcasting, timeoutMinutes, TimeUnit.MINUTES);
            }
            return true;

        } catch (Exception e) {
            System.err.println("Error starting stream: " + e.getMessage());
            return false;
        }
    }

    public synchronized void stopBroadcasting() {
        if (!isBroadcasting) return;

        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(false);
        }

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
        jmdnsInstances.clear();
        isBroadcasting = false;
        currentClassName = "";
        System.out.println("🛑 Stream ended.");
    }

    public boolean isBroadcasting() {
        return isBroadcasting;
    }

    public String getCurrentClassName() {
        return currentClassName;
    }

    @PreDestroy
    public void cleanup() {
        stopBroadcasting();
        scheduler.shutdown();
    }
}