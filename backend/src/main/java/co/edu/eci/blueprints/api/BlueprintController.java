package co.edu.eci.blueprints.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "1. Blueprints (demo)",
        description = "Endpoints didácticos con datos en memoria, usados para probar los scopes rápidamente")
@RestController
@RequestMapping("/api/blueprints")
public class BlueprintController {

    @Operation(
            summary = "Lista los planos de ejemplo",
            description = "Devuelve un conjunto fijo de planos en memoria. Requiere scope `blueprints.read`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Listado de planos",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    [
                      { "id": "b1", "name": "Casa de campo" },
                      { "id": "b2", "name": "Edificio urbano" }
                    ]"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Token ausente, malformado o expirado", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "El token no tiene el scope blueprints.read", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_blueprints.read')")
    public List<Map<String, String>> list() {
        return List.of(
                Map.of("id", "b1", "name", "Casa de campo"),
                Map.of("id", "b2", "name", "Edificio urbano")
        );
    }

    @Operation(
            summary = "Crea un plano de ejemplo",
            description = "Eco del nombre recibido, sin persistencia real. Requiere scope `blueprints.write`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Plano creado",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"id\": \"new\", \"name\": \"Nuevo Plano\" }"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Token ausente, malformado o expirado", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "El token no tiene el scope blueprints.write", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_blueprints.write')")
    public Map<String, String> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Objeto con el nombre del plano",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"name\": \"Nuevo Plano\" }")))
            @RequestBody Map<String, String> in) {
        return Map.of("id", "new", "name", in.getOrDefault("name", "nuevo"));
    }
}