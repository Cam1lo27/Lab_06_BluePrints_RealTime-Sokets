package co.edu.eci.blueprints.realtime;

import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.persistence.BlueprintNotFoundException;
import co.edu.eci.blueprints.services.BlueprintsServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * Colaboración en tiempo real (STOMP).
 * <p>
 * El cliente publica un {@link DrawEvent} en {@code /app/draw}; el servidor guarda el punto
 * en la persistencia y difunde el estado del plano a {@code /topic/blueprints.{author}.{name}}.
 * Cada plano es un tópico distinto, así que los planos quedan aislados entre sí.
 */
@Controller
public class BlueprintRealtimeController {

    private static final Logger log = LoggerFactory.getLogger(BlueprintRealtimeController.class);

    static final int MAX_COORD = 10_000;

    private final BlueprintsServices services;
    private final SimpMessagingTemplate template;

    public BlueprintRealtimeController(BlueprintsServices services, SimpMessagingTemplate template) {
        this.services = services;
        this.template = template;
    }

    public static String topicOf(String author, String name) {
        return "/topic/blueprints." + author + "." + name;
    }

    @MessageMapping("/draw")
    public void onDraw(DrawEvent evt, Principal user) throws BlueprintNotFoundException {
        validate(evt);
        services.addPoint(evt.author(), evt.name(), evt.point().x(), evt.point().y());

        Blueprint bp = services.getBlueprint(evt.author(), evt.name());
        String topic = topicOf(evt.author(), evt.name());
        template.convertAndSend(topic, new BlueprintUpdate(bp.getAuthor(), bp.getName(), bp.getPoints()));

        log.info("[RT] draw user={} plano={}/{} punto=({},{}) -> {} ({} puntos)",
                user != null ? user.getName() : "?", evt.author(), evt.name(),
                evt.point().x(), evt.point().y(), topic, bp.getPoints().size());
    }

    private static void validate(DrawEvent evt) {
        if (evt == null || isBlank(evt.author()) || isBlank(evt.name())) {
            throw new IllegalArgumentException("author y name son obligatorios");
        }
        if (evt.point() == null) {
            throw new IllegalArgumentException("point es obligatorio");
        }
        int x = evt.point().x(), y = evt.point().y();
        if (x < 0 || y < 0 || x > MAX_COORD || y > MAX_COORD) {
            throw new IllegalArgumentException("Coordenadas fuera de rango (0.." + MAX_COORD + ")");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Los errores se devuelven solo al cliente que envió el mensaje, en /user/queue/errors. */
    @MessageExceptionHandler({IllegalArgumentException.class, BlueprintNotFoundException.class})
    @SendToUser("/queue/errors")
    public Map<String, String> onError(Exception e, Principal user) {
        log.warn("[RT] draw rechazado user={}: {}", user != null ? user.getName() : "?", e.getMessage());
        return Map.of("error", e.getMessage());
    }
}
