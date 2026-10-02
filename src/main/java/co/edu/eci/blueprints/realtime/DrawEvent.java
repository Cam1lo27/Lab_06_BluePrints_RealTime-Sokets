package co.edu.eci.blueprints.realtime;

import co.edu.eci.blueprints.model.Point;

/** Mensaje que envía el cliente a {@code /app/draw}: un punto nuevo para un plano. */
public record DrawEvent(String author, String name, Point point) { }
