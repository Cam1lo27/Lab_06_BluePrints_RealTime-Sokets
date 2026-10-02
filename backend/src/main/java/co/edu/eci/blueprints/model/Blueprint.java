package co.edu.eci.blueprints.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Schema(name = "Blueprint", description = "Plano identificado por autor y nombre, con su lista de puntos")
public class Blueprint {

    @Schema(description = "Autor del plano", example = "john")
    private String author;

    @Schema(description = "Nombre del plano", example = "casa")
    private String name;

    @Schema(description = "Puntos que componen el plano")
    private final List<Point> points = new ArrayList<>();

    public Blueprint(String author, String name, List<Point> pts) {
        this.author = author;
        this.name = name;
        if (pts != null) points.addAll(pts);
    }

    public String getAuthor() { return author; }
    public String getName() { return name; }
    public List<Point> getPoints() { return Collections.unmodifiableList(points); }

    public void addPoint(Point p) { points.add(p); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Blueprint bp)) return false;
        return Objects.equals(author, bp.author) && Objects.equals(name, bp.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(author, name);
    }
}