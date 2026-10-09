package backend_r.reciclaje.dashboard;

import backend_r.reciclaje.auth.Role;
import backend_r.reciclaje.auth.Usuario;
import backend_r.reciclaje.auth.UsuarioRepository;
import backend_r.reciclaje.dashboard.DashboardDtos.AwardPointsRequest;
import backend_r.reciclaje.dashboard.DashboardDtos.PointEntryResponse;
import backend_r.reciclaje.dashboard.DashboardDtos.StudentDashboardResponse;
import backend_r.reciclaje.dashboard.DashboardDtos.StudentRegistrationRequest;
import backend_r.reciclaje.dashboard.DashboardDtos.StudentWinnerResponse;
import backend_r.reciclaje.dashboard.DashboardDtos.TeacherStudentResponse;
import backend_r.reciclaje.domain.Alumno;
import backend_r.reciclaje.domain.AlumnoRepository;
import backend_r.reciclaje.domain.Profesor;
import backend_r.reciclaje.domain.ProfesorRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final UsuarioRepository usuarios;
    private final AlumnoRepository alumnos;
    private final ProfesorRepository profesores;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public DashboardService(
            UsuarioRepository usuarios,
            AlumnoRepository alumnos,
            ProfesorRepository profesores,
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.alumnos = alumnos;
        this.profesores = profesores;
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public StudentDashboardResponse studentDashboard(String identificador) {
        Alumno alumno = usuarios.findByIdentificadorIgnoreCase(identificador)
                .filter(user -> user.getRol() == Role.ALUMNO && user.getAlumno() != null)
                .map(Usuario::getAlumno)
                .orElseGet(() -> findStudent(identificador)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Alumno no encontrado")));
        List<PointEntryResponse> movements = jdbc.query("""
                SELECT ID_REGISTRO, PUNTOS, OBSERVACION, FECHA_REGISTRO
                FROM REGISTRO_PUNTOS
                WHERE ID_ALUMNO = ?
                ORDER BY FECHA_REGISTRO DESC, ID_REGISTRO DESC
                """, (rs, rowNum) -> new PointEntryResponse(
                rs.getLong("ID_REGISTRO"),
                rs.getInt("PUNTOS"),
                rs.getString("OBSERVACION"),
                rs.getTimestamp("FECHA_REGISTRO").toLocalDateTime()), alumno.getId());
        long total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(PUNTOS), 0) FROM REGISTRO_PUNTOS WHERE ID_ALUMNO = ?",
                Long.class,
                alumno.getId());
        return new StudentDashboardResponse(alumno.getId(), alumno.getNombreCompleto(), total, movements);
    }

    private Optional<Alumno> findStudent(String identificador) {
        try {
            return alumnos.findById(Long.valueOf(identificador));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public List<TeacherStudentResponse> teacherStudents(String identificador) {
        Profesor teacher = requireTeacher(identificador);
        return jdbc.query("""
                SELECT A.ID_ALUMNO, A.NOMBRE || ' ' || A.APELLIDO AS NOMBRE_COMPLETO,
                       COALESCE(SUM(R.PUNTOS), 0) AS PUNTOS_TOTAL
                FROM COLEGIO_ALUMNO CA
                JOIN ALUMNO A ON A.ID_ALUMNO = CA.ID_ALUMNO
                LEFT JOIN REGISTRO_PUNTOS R ON R.ID_ALUMNO = A.ID_ALUMNO
                WHERE CA.ID_COLEGIO = ?
                GROUP BY A.ID_ALUMNO, A.NOMBRE, A.APELLIDO
                ORDER BY A.APELLIDO, A.NOMBRE
                """, (rs, rowNum) -> new TeacherStudentResponse(
                rs.getLong("ID_ALUMNO"),
                rs.getString("NOMBRE_COMPLETO"),
                rs.getLong("PUNTOS_TOTAL")), teacher.getColegio().getId());
    }

    @Transactional(readOnly = true)
    public StudentWinnerResponse studentWinnerLast30Days(String identificador) {
        Profesor teacher = requireTeacher(identificador);
        return jdbc.query("""
                SELECT A.ID_ALUMNO, A.NOMBRE || ' ' || A.APELLIDO AS NOMBRE_COMPLETO,
                       SUM(R.PUNTOS) AS PUNTOS_TOTAL
                FROM COLEGIO_ALUMNO CA
                JOIN ALUMNO A ON A.ID_ALUMNO = CA.ID_ALUMNO
                JOIN REGISTRO_PUNTOS R ON R.ID_ALUMNO = A.ID_ALUMNO
                    AND R.FECHA_REGISTRO >= CURRENT_DATE - 30
                    AND R.FECHA_REGISTRO < CURRENT_DATE + 1
                WHERE CA.ID_COLEGIO = ?
                GROUP BY A.ID_ALUMNO, A.NOMBRE, A.APELLIDO
                ORDER BY PUNTOS_TOTAL DESC, A.NOMBRE, A.APELLIDO, A.ID_ALUMNO
                FETCH FIRST 1 ROW ONLY
                """, (rs, rowNum) -> new StudentWinnerResponse(
                rs.getLong("ID_ALUMNO"),
                rs.getString("NOMBRE_COMPLETO"),
                rs.getLong("PUNTOS_TOTAL")), teacher.getColegio().getId())
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public TeacherStudentResponse registerStudent(String identificador, StudentRegistrationRequest request) {
        Profesor teacher = requireTeacher(identificador);
        Long salonId = jdbc.queryForObject(
                "SELECT ID_SALON FROM PROFESOR WHERE ID_PROFESOR = ?",
                Long.class,
                teacher.getId());
        logger.info("Salón consultado para profesor {}: {}",
                teacher.getId(), salonId == null ? "NULL" : salonId);
        if (salonId == null) {
            var databaseContext = jdbc.queryForMap("""
                    SELECT SYS_CONTEXT('USERENV', 'DB_NAME') AS DB_NAME,
                           SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS SERVICE_NAME,
                           SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS CURRENT_SCHEMA
                    FROM DUAL
                    """);
            logger.error("No se encontró salón para el profesor {}. Contexto Oracle efectivo: {}",
                    teacher.getId(), databaseContext);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "La consulta del backend no encontró salón para el profesor con ID "
                            + teacher.getId()
                            + ". Conexión efectiva: DB_NAME=" + databaseContext.get("DB_NAME")
                            + ", SERVICE_NAME=" + databaseContext.get("SERVICE_NAME")
                            + ", ESQUEMA=" + databaseContext.get("CURRENT_SCHEMA")
                            + ". Compara estos valores con la conexión de SQL Developer.");
        }
        long studentId = generateStudentId();
        String studentIdentifier = Long.toString(studentId);

        Alumno student = alumnos.saveAndFlush(new Alumno(
                studentId,
                request.nombre().trim(),
                request.apellido().trim(),
                salonId));
        byte[] randomSecret = new byte[32];
        secureRandom.nextBytes(randomSecret);
        String generatedSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomSecret);
        usuarios.saveAndFlush(new Usuario(
                studentIdentifier,
                passwordEncoder.encode(generatedSecret),
                Role.ALUMNO,
                student,
                null));
        try {
            jdbc.update("INSERT INTO COLEGIO_ALUMNO (ID_COLEGIO, ID_ALUMNO) VALUES (?, ?)",
                    teacher.getColegio().getId(), student.getId());
        } catch (DataIntegrityViolationException exception) {
            logger.error("No se pudo asociar el alumno {} al colegio {}",
                    student.getId(), teacher.getColegio().getId(), exception);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo asociar el alumno al colegio");
        }

        return new TeacherStudentResponse(student.getId(), student.getNombreCompleto(), 0);
    }

    private long generateStudentId() {
        for (int attempt = 0; attempt < 100; attempt++) {
            long candidate = secureRandom.nextInt(10_000_000, 100_000_000);
            String identifier = Long.toString(candidate);
            if (!alumnos.existsById(candidate)
                    && !usuarios.existsByIdentificadorIgnoreCase(identifier)
                    && !profesores.existsById(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo generar un ID único");
    }

    @Transactional
    public void awardPoints(String identificador, AwardPointsRequest request) {
        Profesor teacher = requireTeacher(identificador);
        Integer studentCount = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM COLEGIO_ALUMNO
                WHERE ID_COLEGIO = ? AND ID_ALUMNO = ?
                """, Integer.class, teacher.getColegio().getId(), request.alumnoId());
        if (studentCount == null || studentCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El alumno no pertenece a este colegio");
        }
        jdbc.update("""
                INSERT INTO REGISTRO_PUNTOS
                    (ID_ALUMNO, ID_PROFESOR, PUNTOS, OBSERVACION, FECHA_REGISTRO)
                VALUES (?, ?, ?, ?, CURRENT_DATE)
                """, request.alumnoId(), teacher.getId(), request.puntos(), request.motivo().trim());
    }

    private Profesor requireTeacher(String identificador) {
        Profesor teacher;
        try {
            teacher = profesores.findById(Long.valueOf(identificador))
                    .filter(Profesor::isActivo)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN, "Se requiere una cuenta docente activa"));
        } catch (NumberFormatException exception) {
            teacher = profesores.findByCorreoIgnoreCase(identificador)
                    .filter(Profesor::isActivo)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN, "Se requiere una cuenta docente activa"));
        }
        return teacher;
    }
}
