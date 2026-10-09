package backend_r.reciclaje.dashboard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record StudentRegistrationRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido) {
    }

    public record AwardPointsRequest(
            @NotNull @Positive Long alumnoId,
            @NotNull @Positive @Max(10000) Integer puntos,
            @NotBlank @Size(max = 300) String motivo) {
    }

    public record PointEntryResponse(
            Long id,
            Integer puntos,
            String motivo,
            LocalDateTime fecha) {
    }

    public record StudentDashboardResponse(
            Long alumnoId,
            String nombreCompleto,
            long puntosTotal,
            List<PointEntryResponse> movimientos) {
    }

    public record TeacherStudentResponse(
            Long alumnoId,
            String nombreCompleto,
            long puntosTotal) {
    }

    public record StudentWinnerResponse(
            Long alumnoId,
            String nombreCompleto,
            long puntosTotal) {
    }
}
