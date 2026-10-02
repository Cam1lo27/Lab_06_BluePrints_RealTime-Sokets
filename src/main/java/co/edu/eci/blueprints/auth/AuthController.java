package co.edu.eci.blueprints.auth;

import co.edu.eci.blueprints.security.InMemoryUserService;
import co.edu.eci.blueprints.security.RsaKeyProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Tag(name = "1. Autenticación",
     description = "Endpoint didáctico que emite los JWT usados por el resto de la API")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtEncoder encoder;
    private final InMemoryUserService userService;
    private final RsaKeyProperties props;

    public AuthController(JwtEncoder encoder, InMemoryUserService userService, RsaKeyProperties props) {
        this.encoder = encoder;
        this.userService = userService;
        this.props = props;
    }

    @Schema(name = "LoginRequest", description = "Credenciales del usuario")
    public record LoginRequest(
        @Schema(description = "Usuario registrado en memoria", example = "student",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @Schema(description = "Contraseña en texto plano (se compara contra el hash almacenado)",
                example = "student123", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
    ) {}

    @Schema(name = "TokenResponse", description = "Token de acceso emitido por el servidor")
    public record TokenResponse(
        @Schema(description = "JWT firmado en RS256 con la llave privada de la aplicación")
        String access_token,

        @Schema(description = "Tipo de token", example = "Bearer")
        String token_type,

        @Schema(description = "Vigencia del token en segundos", example = "3600")
        long expires_in
    ) {}

    @Operation(
        summary = "Inicia sesión y emite un JWT",
        description = """
            Valida las credenciales contra el servicio de usuarios en memoria y, si son
            correctas, firma un JWT (RS256) con las claims `iss`, `sub`, `iat`, `exp` y
            `scope`. El scope emitido es fijo: `blueprints.read blueprints.write`.

            **Endpoint público:** no requiere token (sería imposible, es el que lo entrega).""")
    @SecurityRequirements // vacío a propósito: anula el requisito global de OpenApiConfig
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token emitido correctamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TokenResponse.class),
                examples = @ExampleObject(name = "Token válido", value = """
                    {
                      "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "token_type": "Bearer",
                      "expires_in": 3600
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Usuario o contraseña inválidos",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{ \"error\": \"invalid_credentials\" }")))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (!userService.isValid(req.username(), req.password())) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }

        Instant now = Instant.now();
        long ttl = props.tokenTtlSeconds() != null ? props.tokenTtlSeconds() : 3600;
        Instant exp = now.plusSeconds(ttl);

        String scope = "blueprints.read blueprints.write";

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(req.username())
                .claim("scope", scope)
                .build();

        JwsHeader jws = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = this.encoder.encode(JwtEncoderParameters.from(jws, claims)).getTokenValue();

        return ResponseEntity.ok(new TokenResponse(token, "Bearer", ttl));
    }
}