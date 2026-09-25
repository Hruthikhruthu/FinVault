package com.finvault.websocket;

import com.finvault.common.service.WebSocketMetrics;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketSessionMetrics implements WebSocketMetrics {
    private final AtomicInteger activeSessions = new AtomicInteger();

    @Override
    public int activeSessions() {
        return activeSessions.get();
    }

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        activeSessions.incrementAndGet();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        activeSessions.updateAndGet(current -> Math.max(0, current - 1));
    }
}
