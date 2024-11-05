package com.example.logMonitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;

@Configuration
public class WatchServiceConfig {

    @Bean
    public WatchService watchService() throws IOException {
        return FileSystems.getDefault().newWatchService();
    }
}
