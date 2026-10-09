package backend_r.reciclaje.dashboard;

import backend_r.reciclaje.dashboard.DashboardDtos.AwardPointsRequest;
import backend_r.reciclaje.dashboard.DashboardDtos.StudentDashboardResponse;
import backend_r.reciclaje.dashboard.DashboardDtos.StudentRegistrationRequest;
import backend_r.reciclaje.dashboard.DashboardDtos.StudentWinnerResponse;
import backend_r.reciclaje.dashboard.DashboardDtos.TeacherStudentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/alumno/puntos")
    public StudentDashboardResponse studentPoints(Authentication authentication) {
        return dashboardService.studentDashboard(authentication.getName());
    }

    @GetMapping("/docente/alumnos")
    public List<TeacherStudentResponse> teacherStudents(Authentication authentication) {
        return dashboardService.teacherStudents(authentication.getName());
    }

    @GetMapping("/docente/alumnos/ganador-30-dias")
    public StudentWinnerResponse studentWinnerLast30Days(Authentication authentication) {
        return dashboardService.studentWinnerLast30Days(authentication.getName());
    }

    @PostMapping("/docente/alumnos")
    public TeacherStudentResponse registerStudent(
            Authentication authentication,
            @Valid @RequestBody StudentRegistrationRequest request) {
        return dashboardService.registerStudent(authentication.getName(), request);
    }

    @PostMapping("/docente/puntos")
    public void awardPoints(
            Authentication authentication,
            @Valid @RequestBody AwardPointsRequest request) {
        dashboardService.awardPoints(authentication.getName(), request);
    }
}
