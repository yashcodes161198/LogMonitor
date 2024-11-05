package com.example.logMonitor.service;

import jakarta.annotation.PostConstruct;
import lombok.Synchronized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LogFileMonitorService extends TextWebSocketHandler {

    private final WatchService watchService;


    private static final String LOG_FILE_PATH = "C:\\Users\\yash\\Desktop\\log.txt";
    private Path filePath = Paths.get(LOG_FILE_PATH);
    private long lastKnownPosition = 0;
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private List<String> logLines = new CopyOnWriteArrayList<>();
//    private List<String> newLines = new ArrayList<>();

    @Autowired
    public LogFileMonitorService(WatchService watchService) {
        this.watchService = watchService;
    }

    @PostConstruct
    public void startFileMonitor() {
        try {
            // Register the directory of the file with the WatchService
            filePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            System.out.println("Monitoring started for file (append-only): " + filePath);

            // Start a separate thread to handle the file monitoring
            monitorLogFile();
            Thread fileMonitorThread = new Thread(this::processEvents);
            fileMonitorThread.setDaemon(true); // Ensures the thread exits when the application stops
            fileMonitorThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processEvents() {
        while (true) {
            try {
                WatchKey key = watchService.take(); // Wait for a watch key to be signaled

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // Get the file that was affected by the event
                    Path changedFile = (Path) event.context();
                    if (filePath.endsWith(changedFile) && kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        monitorLogFile();
                    }
                }

                // Reset the key to be ready for the next set of events
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("File monitoring thread interrupted.");
                break;
            }
        }
    }

    public void monitorLogFile() {
        try (RandomAccessFile file = new RandomAccessFile(LOG_FILE_PATH, "r")) {
            file.seek(lastKnownPosition);
            String line;
            List<String> newLines = new ArrayList<>();
            while ((line = file.readLine()) != null) {
                newLines.add(line);
            }
            if(!newLines.isEmpty()){
                broadcastNewLines(newLines);
                logLines.addAll(newLines);
            }
//            messagingTemplate.convertAndSend("/topic/log", newLines);
            lastKnownPosition = file.getFilePointer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastNewLines(List<String> newLines) {
        String message = String.join("\n", newLines);
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        int numberOfLines = logLines.size();
        int n = Math.min(10, numberOfLines);
        List<String> newLines = logLines.subList(logLines.size() - n, logLines.size());
        String message = String.join("\n", newLines);
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

//    @Scheduled(fixedDelay = 1000)
//    public void pollForChanges() {
//        monitorLogFile();
//    }

    @Scheduled(fixedDelay = 10000)
    @Synchronized
    public void trimLogLines() {
        int numberOfLines = logLines.size();
        int n = Math.min(10, numberOfLines);
        if(numberOfLines > 100){
            logLines = logLines.subList(logLines.size() - n, logLines.size());
        }
    }


}
