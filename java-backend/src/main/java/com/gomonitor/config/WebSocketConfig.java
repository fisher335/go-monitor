package com.gomonitor.config;

import com.gomonitor.hub.MonitorHub;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** WebSocket 配置 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MonitorHub monitorHub;

    public WebSocketConfig(MonitorHub monitorHub) {
        this.monitorHub = monitorHub;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(monitorHub, "/api/ws")
                .setAllowedOrigins("*");
    }
}
