package co.edu.eci.blueprints.realtime;

import co.edu.eci.blueprints.model.Point;

import java.util.List;

/**
 * Mensaje que el servidor publica en {@code /topic/blueprints.{author}.{name}}.
 * Lleva TODOS los puntos del plano ya guardados (no solo el último), así cada
 * cliente puede repintar el estado completo y nunca queda desincronizado.
 */
public record BlueprintUpdate(String author, String name, List<Point> points) { }
