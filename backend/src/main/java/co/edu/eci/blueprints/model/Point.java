package co.edu.eci.blueprints.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Point", description = "Coordenada de un plano")
public record Point(
        @Schema(description = "Coordenada X", example = "10") int x,
        @Schema(description = "Coordenada Y", example = "20") int y
) { }