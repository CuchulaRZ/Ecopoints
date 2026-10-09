package backend_r.reciclaje.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 150) String identificador,
            @NotBlank @Size(max = 100) String password) {
    }

    public record IdLoginRequest(
            @NotBlank @Size(max = 80) String identificador) {
    }

    public record CreateUserRequest(
            @NotBlank
            @Size(max = 150)
            String identificador,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotNull Role rol,
            Long alumnoId,
            Long colegioId,
            @Size(max = 80) String nombre,
            @Size(max = 100) String apellido,
            @Positive Long salonId) {
        public CreateUserRequest(
                String identificador,
                String password,
                Role rol,
                Long alumnoId,
                Long colegioId) {
            this(identificador, password, rol, alumnoId, colegioId, null, null, null);
        }
    }

    public record UserResponse(
            Long id,
            String identificador,
            Role rol,
            Long alumnoId,
            Long colegioId,
            String nombreMostrar) {
    }
}
