package backend_r.reciclaje.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend_r.reciclaje.auth.AuthDtos.CreateUserRequest;
import backend_r.reciclaje.auth.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthService authService;

    @Test
    void adminCanLogInAndReadItsSessionProfile() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"identificador":"test-admin","password":"test-password-12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("TEST-ADMIN"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void studentCanLogInUsingItsIdentifier() throws Exception {
        long studentId = System.nanoTime();
        jdbc.update("INSERT INTO ALUMNO (ID_ALUMNO, NOMBRE, APELLIDO, ID_SALON) VALUES (?, ?, ?, ?)",
                studentId, "Ana", "Verde", 321L);
        authService.createUser(new CreateUserRequest(
                Long.toString(studentId), "student-test-password-123", Role.ALUMNO, studentId, null));

        MvcResult login = mockMvc.perform(post("/api/auth/login-id")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"identificador":"%s"}
                                """.formatted(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ALUMNO"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value(Long.toString(studentId)));
    }

    @Test
    void existingStudentCanLogInWithoutAUsuarioAccount() throws Exception {
        long studentId = System.nanoTime();
        jdbc.update("INSERT INTO ALUMNO (ID_ALUMNO, NOMBRE, APELLIDO, ID_SALON) VALUES (?, ?, ?, ?)",
                studentId, "Pepito", "Palote", 321L);

        MvcResult login = mockMvc.perform(post("/api/auth/login-id")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"identificador":"%s"}
                                """.formatted(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentId))
                .andExpect(jsonPath("$.alumnoId").value(studentId))
                .andExpect(jsonPath("$.rol").value("ALUMNO"))
                .andExpect(jsonPath("$.nombreMostrar").value("Pepito Palote"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ALUMNO"))
                .andExpect(jsonPath("$.nombreMostrar").value("Pepito Palote"));
    }

    @Test
    void professorTakesPrecedenceWhenProfessorAndStudentIdsOverlap() throws Exception {
        long overlappingId = Math.abs(System.nanoTime());
        long collegeId = Math.abs(System.nanoTime());
        jdbc.update("INSERT INTO COLEGIO (ID_COLEGIO, NOMBRE) VALUES (?, ?)", collegeId, "Colegio Verde");
        jdbc.update(
                """
                        INSERT INTO PROFESOR
                            (ID_PROFESOR, ID_COLEGIO, ID_SALON, NOMBRE, APELLIDO, CORREO, PASSWORD_HASH, ESTADO)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                overlappingId, collegeId, 321L, "Patricia", "Verde",
                "patricia-" + overlappingId + "@example.test", "hash", "A");
        jdbc.update("INSERT INTO ALUMNO (ID_ALUMNO, NOMBRE, APELLIDO, ID_SALON) VALUES (?, ?, ?, ?)",
                overlappingId, "Ana", "Estudiante", 321L);

        MvcResult login = mockMvc.perform(post("/api/auth/login-id")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"identificador":"%s"}
                                """.formatted(overlappingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PROFESOR"))
                .andExpect(jsonPath("$.nombreMostrar").value("Patricia Verde"))
                .andExpect(jsonPath("$.alumnoId").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PROFESOR"))
                .andExpect(jsonPath("$.nombreMostrar").value("Patricia Verde"));
    }

    @Test
    void adminCanCreateProfessorThatAuthenticatesAgainstProfessorTable() throws Exception {
        long collegeId = Math.abs(System.nanoTime());
        String email = "teacher-" + collegeId + "@example.test";
        jdbc.update("INSERT INTO COLEGIO (ID_COLEGIO, NOMBRE) VALUES (?, ?)", collegeId, "Colegio Verde");
        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"identificador":"test-admin","password":"test-password-12345"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);

        mockMvc.perform(post("/api/admin/users")
                        .session(adminSession)
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "identificador":"%s",
                                  "password":"teacher-test-password-123",
                                  "rol":"PROFESOR",
                                  "colegioId":%d,
                                  "salonId":321,
                                  "nombre":"Patricia",
                                  "apellido":"Verde"
                                }
                                """.formatted(email, collegeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PROFESOR"))
                .andExpect(jsonPath("$.nombreMostrar").value("Patricia Verde"))
                .andExpect(jsonPath("$.colegioId").value(collegeId));

        long professorId = jdbc.queryForObject(
                "SELECT ID_PROFESOR FROM PROFESOR WHERE CORREO = ?",
                Long.class,
                email);
        MvcResult teacherLogin = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"identificador":"%s","password":"teacher-test-password-123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(professorId))
                .andExpect(jsonPath("$.rol").value("PROFESOR"))
                .andReturn();
        MockHttpSession teacherSession = (MockHttpSession) teacherLogin.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(professorId));
    }

    @Test
    void adminCannotUsePasswordlessIdLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login-id")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"identificador":"test-admin"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
