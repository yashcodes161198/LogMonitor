// src/LogMonitor.js
import React, { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';

const Container = styled.div`
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-height: 500px;
`;

const LogContainer = styled.div`
  overflow-y: scroll;
  flex: 1;
  padding: 10px;
  background-color: #f9f9f9;
  border: 1px solid #ccc;
`;

const LogLine = styled.div`
  font-family: monospace;
  font-size: 14px;
  padding: 4px 0;
`;

const PlayPauseButton = styled.button`
  align-self: center;
  padding: 8px 16px;
  font-size: 16px;
  cursor: pointer;
  background-color: ${props => (props.isPaused ? '#2563EB' : '#000000')};
  color: white;
  border: none;
  border-radius: 4px;
  margin-top: 10px;
`;

export default function LogMonitor() {
  const [logs, setLogs] = useState([]);
  const [isPaused, setIsPaused] = useState(false);
  const logContainerRef = useRef(null);
  const websocketRef = useRef(null);
  const isAutoScroll = useRef(true);

  useEffect(() => {
    // Initialize WebSocket connection once when the component mounts
    websocketRef.current = new WebSocket('ws://localhost:8082/log');

    websocketRef.current.onmessage = (event) => {
      if (!isPaused) {
        const newLines = event.data.split('\n');
        setLogs(prevLogs => [...prevLogs, ...newLines]);
      }
    };

    // Cleanup WebSocket on component unmount
    return () => websocketRef.current.close();
  }, []); // Empty dependency array ensures this runs only once

  const togglePlayPause = () => {
    setIsPaused(prev => {
      const newPauseState = !prev;
      if (websocketRef.current && websocketRef.current.readyState === WebSocket.OPEN) {
        const actionMessage = JSON.stringify({ action: newPauseState ? "pause" : "resume" });
        websocketRef.current.send(actionMessage);
      }
      return newPauseState;
    });
  };

  const handleScroll = () => {
    const { scrollTop, scrollHeight, clientHeight } = logContainerRef.current;
    isAutoScroll.current = scrollTop + clientHeight >= scrollHeight - 5;
  };

  useEffect(() => {
    if (isAutoScroll.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [logs]);

  return (
    <Container>
      <LogContainer ref={logContainerRef} onScroll={handleScroll}>
        {logs.map((log, index) => (
          <LogLine key={index}>{log}</LogLine>
        ))}
      </LogContainer>
      <PlayPauseButton isPaused={isPaused} onClick={togglePlayPause}>
        {isPaused ? 'Play' : 'Pause'}
      </PlayPauseButton>
    </Container>
  );
}
