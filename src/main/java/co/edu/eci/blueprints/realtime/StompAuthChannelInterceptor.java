package co.edu.eci.blueprints.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Exige el mismo JWT de la API REST en el frame STOMP CONNECT
 * (header {@code Authorization: Bearer <token>}). Sin token válido no hay conexión,
 * así nadie puede escribir puntos en la base sin haber iniciado sesión.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private final JwtDecoder jwtDecoder;

    public StompAuthChannelInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("[RT] CONNECT rechazado: falta el header Authorization (session={})", accessor.getSessionId());
            throw new MessageDeliveryException("Falta el token JWT (Authorization: Bearer <token>)");
        }

        try {
            Jwt jwt = jwtDecoder.decode(header.substring("Bearer ".length()));
            List<SimpleGrantedAuthority> authorities = Arrays.stream(
                            String.valueOf(jwt.getClaims().getOrDefault("scope", "")).split(" "))
                    .filter(s -> !s.isBlank())
                    .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                    .toList();
            accessor.setUser(new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, authorities));
            return message;
        } catch (JwtException e) {
            log.warn("[RT] CONNECT rechazado: token inválido ({}) (session={})", e.getMessage(), accessor.getSessionId());
            throw new MessageDeliveryException("Token JWT inválido o expirado");
        }
    }
}
