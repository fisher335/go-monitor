package com.gomonitor.hub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomonitor.model.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** WebSocket Hub — 管理客户端连接并广播数据 */
@Component
public class MonitorHub extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MonitorHub.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("📡 WebSocket 客户端连接（当前 {} 个）", sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("📡 WebSocket 客户端断开（当前 {} 个）", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 接收客户端消息（目前不做处理）
    }

    /** 向所有客户端广播数据 */
    public void broadcast(List<ServerStatus> servers) {
        if (sessions.isEmpty()) return;

        try {
            var payload = Map.of(
                    "servers", servers,
                    "updated", System.currentTimeMillis()
            );
            String json = mapper.writeValueAsString(payload);
            TextMessage msg = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(msg);
                    }
                } catch (IOException e) {
                    log.warn("发送消息失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("广播数据失败: {}", e.getMessage());
        }
    }

    public int getClientCount() {
        return sessions.size();
    }
}
