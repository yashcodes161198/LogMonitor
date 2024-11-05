package com.example.logMonitor.config;

import com.example.logMonitor.service.LogFileMonitorService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LogFileMonitorService logFileMonitorService;

    public WebSocketConfig(LogFileMonitorService logFileMonitorService) {
        this.logFileMonitorService = logFileMonitorService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logFileMonitorService, "/log").setAllowedOrigins("*");
    }
}

