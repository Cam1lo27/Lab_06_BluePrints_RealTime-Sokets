package co.edu.eci.blueprints.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/** Logs de observabilidad: conexión, suscripción y desconexión de clientes STOMP. */
@Component
public class WebSocketEventsLogger {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventsLogger.class);

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());
        log.info("[RT] conectado user={} session={}", userOf(acc), acc.getSessionId());
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());
        log.info("[RT] suscrito user={} session={} destino={}", userOf(acc), acc.getSessionId(), acc.getDestination());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());
        log.info("[RT] desconectado user={} session={} estado={}", userOf(acc), event.getSessionId(), event.getCloseStatus());
    }

    private static String userOf(StompHeaderAccessor acc) {
        return acc.getUser() != null ? acc.getUser().getName() : "?";
    }
}
