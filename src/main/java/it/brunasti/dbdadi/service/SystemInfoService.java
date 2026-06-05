package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.SystemInfoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class SystemInfoService implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${app.version:unknown}")
    private String appVersion;

    private LocalDateTime startupTime;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        startupTime = LocalDateTime.now();
    }

    public SystemInfoDto getInfo() {
        LocalDateTime now = LocalDateTime.now();
        long uptime = startupTime != null ? Duration.between(startupTime, now).getSeconds() : 0;
        return SystemInfoDto.builder()
                .appVersion(appVersion)
                .startupTime(startupTime)
                .serverTime(now)
                .uptimeSeconds(uptime)
                .javaVersion(System.getProperty("java.version"))
                .javaVendor(System.getProperty("java.vendor"))
                .osName(System.getProperty("os.name"))
                .osVersion(System.getProperty("os.version"))
                .build();
    }
}
