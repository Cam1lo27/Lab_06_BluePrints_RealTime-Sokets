package co.edu.eci.blueprints.controllers;

import co.edu.eci.blueprints.dto.ApiResponse;
import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import co.edu.eci.blueprints.persistence.BlueprintNotFoundException;
import co.edu.eci.blueprints.persistence.BlueprintPersistenceException;
import co.edu.eci.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "2. Blueprints (Lab P1)",
        description = "Operaciones sobre planos, protegidas por scopes del JWT (blueprints.read / blueprints.write)")
@RestController
@RequestMapping("/api/v1/blueprints")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    @Operation(
            summary = "Obtener todos los blueprints",
            description = "Requiere un token con scope `blueprints.read`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Listado de planos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Token ausente, malformado o expirado", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "El token no tiene el scope blueprints.read", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_blueprints.read')")
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(services.getAllBlueprints()));
    }

    @Operation(
            summary = "Obtener los blueprints de un autor",
            description = "Requiere un token con scope `blueprints.read`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Planos del autor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Token ausente o expirado", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Falta el scope blueprints.read", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "El autor no tiene planos registrados", content = @Content)
    })
    @GetMapping("/{author}")
    @PreAuthorize("hasAuthority('SCOPE_blueprints.read')")
    public ResponseEntity<ApiResponse<Set<Blueprint>>> byAuthor(
            @Parameter(description = "Autor de los planos", example = "john")
            @PathVariable String author) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(services.getBlueprintsByAuthor(author)));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @Operation(
            summary = "Obtener un blueprint específico",
            description = "Requiere un token con scope `blueprints.read`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Plano encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Token ausente o expirado", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Falta el scope blueprints.read", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "No existe un plano con ese autor y nombre", content = @Content)
    })
    @GetMapping("/{author}/{bpname}")
    @PreAuthorize("hasAuthority('SCOPE_blueprints.read')")
    public ResponseEntity<ApiResponse<Blueprint>> byAuthorAndName(
            @Parameter(description = "Autor del plano", example = "john") @PathVariable String author,
            @Parameter(description = "Nombre del plano", example = "casa") @PathVariable String bpname) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(services.getBlueprint(author, bpname)));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, e.getMessage()));
        }
    }

    @Operation(
            summary = "Crear un nuevo blueprint",
            description = "Requiere un token con scope `blueprints.write`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Creado",
                    content = @Content(schema = @Schema(implementation = Blueprint.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Datos inválidos o el plano ya existe", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Token ausente o expirado", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Falta el scope blueprints.write", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_blueprints.write')")
    public ResponseEntity<ApiResponse<Blueprint>> add(@Valid @RequestBody NewBlueprintRequest req) {
        try {
            Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
            services.addNewBlueprint(bp);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(bp));
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @Operation(
            summary = "Agregar un punto a un blueprint",
            description = "Requiere un token con scope `blueprints.write`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202", description = "Punto agregado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Token ausente o expirado", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Falta el scope blueprints.write", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "No existe un plano con ese autor y nombre", content = @Content)
    })
    @PutMapping("/{author}/{bpname}/points")
    @PreAuthorize("hasAuthority('SCOPE_blueprints.write')")
    public ResponseEntity<ApiResponse<Void>> addPoint(
            @Parameter(description = "Autor del plano", example = "john") @PathVariable String author,
            @Parameter(description = "Nombre del plano", example = "casa") @PathVariable String bpname,
            @RequestBody Point p) {
        try {
            services.addPoint(author, bpname, p.x(), p.y());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.accepted(null));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, e.getMessage()));
        }
    }

    @Schema(name = "NewBlueprintRequest", description = "Datos necesarios para crear un blueprint")
    public record NewBlueprintRequest(
            @NotBlank
            @Schema(description = "Autor del plano", example = "john")
            String author,

            @NotBlank
            @Schema(description = "Nombre del plano", example = "casa")
            String name,

            @Valid
            @Schema(description = "Puntos iniciales del plano (puede ir vacío)")
            List<Point> points
    ) { }
}