package co.edu.eci.blueprints.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiResponse", description = "Envoltorio estándar de todas las respuestas de la API")
public record ApiResponse<T>(
        @Schema(description = "Código de estado de negocio", example = "200") int code,
        @Schema(description = "Mensaje descriptivo", example = "execute ok") String message,
        @Schema(description = "Cuerpo de la respuesta (puede ser null en errores)") T data) {

    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(200, "execute ok", data); }
    public static <T> ApiResponse<T> created(T data) { return new ApiResponse<>(201, "resource created", data); }
    public static <T> ApiResponse<T> accepted(T data) { return new ApiResponse<>(202, "resource updated", data); }
    public static <T> ApiResponse<T> error(int code, String message) { return new ApiResponse<>(code, message, null); }
}