package backend_r.reciclaje.auth;

import backend_r.reciclaje.auth.AuthDtos.CreateUserRequest;
import backend_r.reciclaje.auth.AuthDtos.UserResponse;
import backend_r.reciclaje.domain.Alumno;
import backend_r.reciclaje.domain.AlumnoRepository;
import backend_r.reciclaje.domain.Colegio;
import backend_r.reciclaje.domain.ColegioRepository;
import backend_r.reciclaje.domain.Profesor;
import backend_r.reciclaje.domain.ProfesorRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final AlumnoRepository alumnos;
    private final ColegioRepository colegios;
    private final ProfesorRepository profesores;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UsuarioRepository usuarios,
            AlumnoRepository alumnos,
            ColegioRepository colegios,
            ProfesorRepository profesores,
            PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.alumnos = alumnos;
        this.colegios = colegios;
        this.profesores = profesores;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(String identificador) {
        Optional<Profesor> profesor = findProfessor(identificador);
        if (profesor.isPresent()) {
            return toResponse(profesor.get());
        }
        Optional<Usuario> usuario = usuarios.findByIdentificadorIgnoreCase(identificador);
        if (usuario.isPresent()) {
            return toResponse(usuario.get());
        }
        Optional<Alumno> alumno = findStudent(identificador);
        if (alumno.isPresent()) {
            return toResponse(alumno.get());
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La contraseña no puede superar 72 bytes UTF-8");
        }
        if (request.password().length() < 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 12 caracteres");
        }

        if (request.rol() == Role.PROFESOR) {
            return createProfessor(request);
        }

        String identificador = normalizeIdentifier(request.identificador());
        if (!identificador.matches("^[A-Z0-9._-]{3,80}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El identificador solo puede contener letras, numeros, punto, guion y guion bajo");
        }
        if (usuarios.existsByIdentificadorIgnoreCase(identificador)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El identificador ya está registrado");
        }

        Alumno alumno = null;
        Colegio colegio = null;
        switch (request.rol()) {
            case ALUMNO -> {
                if (request.alumnoId() == null || request.colegioId() != null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "La cuenta de alumno requiere solo alumnoId");
                }
                if (profesores.existsById(request.alumnoId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El ID ya pertenece a un profesor");
                }
                alumno = alumnos.findById(request.alumnoId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
            }
            case PROFESOR -> throw new IllegalStateException("Los profesores se guardan en la tabla PROFESOR");
            case INSTITUCION -> {
                if (request.colegioId() == null || request.alumnoId() != null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "La cuenta de institucion requiere solo colegioId");
                }
                colegio = colegios.findById(request.colegioId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colegio no encontrado"));
            }
            case ADMIN -> {
                if (request.alumnoId() != null || request.colegioId() != null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "La cuenta ADMIN no se vincula a alumno ni colegio");
                }
            }
        }

        Usuario usuario = new Usuario(
                identificador,
                passwordEncoder.encode(request.password()),
                request.rol(),
                alumno,
                colegio);
        try {
            return toResponse(usuarios.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya existe o la asociación no es válida");
        }
    }

    private UserResponse createProfessor(CreateUserRequest request) {
        String correo = request.identificador().trim().toLowerCase(Locale.ROOT);
        if (!correo.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$")
                || request.colegioId() == null
                || request.alumnoId() != null
                || request.nombre() == null
                || request.nombre().isBlank()
                || request.apellido() == null
                || request.apellido().isBlank()
                || request.salonId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cuenta de profesor requiere correo, nombre, apellido, colegioId y salonId");
        }
        if (usuarios.existsByIdentificadorIgnoreCase(correo)
                || profesores.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        Colegio colegio = colegios.findById(request.colegioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colegio no encontrado"));
        try {
            return toResponse(profesores.saveAndFlush(new Profesor(
                    colegio,
                    request.nombre().trim(),
                    request.apellido().trim(),
                    correo,
                    passwordEncoder.encode(request.password()),
                    request.salonId())));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El profesor ya existe o la asociación no es válida");
        }
    }

    @Transactional
    public UserResponse createBootstrapAdmin(String identificador, String password) {
        String normalizedIdentifier = normalizeIdentifier(identificador);
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("La contraseña bootstrap debe tener como maximo 72 bytes UTF-8");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("La contraseña bootstrap debe tener al menos 12 caracteres");
        }
        if (usuarios.existsByIdentificadorIgnoreCase(normalizedIdentifier)) {
            Usuario existing = usuarios.findByIdentificadorIgnoreCase(normalizedIdentifier).orElseThrow();
            if (existing.getRol() != Role.ADMIN) {
                throw new IllegalStateException("El identificador configurado para bootstrap ya pertenece a otra cuenta");
            }
            return toResponse(existing);
        }
        if (usuarios.existsByRol(Role.ADMIN)) {
            throw new IllegalStateException(
                    "Ya existe un administrador; el identificador de bootstrap configurado no coincide");
        }
        return createUser(new CreateUserRequest(normalizedIdentifier, password, Role.ADMIN, null, null));
    }

    private String normalizeIdentifier(String identificador) {
        if (identificador == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador es obligatorio");
        }
        return identificador.trim().toUpperCase(Locale.ROOT);
    }

    private UserResponse toResponse(Usuario usuario) {
        Long alumnoId = usuario.getAlumno() == null ? null : usuario.getAlumno().getId();
        Long colegioId = usuario.getColegio() == null ? null : usuario.getColegio().getId();
        String nombreMostrar;
        if (usuario.getAlumno() != null) {
            nombreMostrar = usuario.getAlumno().getNombreCompleto();
        } else if (usuario.getColegio() != null) {
            nombreMostrar = usuario.getColegio().getNombre();
        } else {
            nombreMostrar = usuario.getIdentificador();
        }
        return new UserResponse(usuario.getId(), usuario.getIdentificador(), usuario.getRol(),
                alumnoId, colegioId, nombreMostrar);
    }

    private Optional<Profesor> findProfessor(String identificador) {
        try {
            return profesores.findById(Long.valueOf(identificador));
        } catch (NumberFormatException exception) {
            return profesores.findByCorreoIgnoreCase(identificador);
        }
    }

    private Optional<Alumno> findStudent(String identificador) {
        try {
            return alumnos.findById(Long.valueOf(identificador));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private UserResponse toResponse(Alumno alumno) {
        return new UserResponse(
                alumno.getId(),
                alumno.getId().toString(),
                Role.ALUMNO,
                alumno.getId(),
                null,
                alumno.getNombreCompleto());
    }

    private UserResponse toResponse(Profesor profesor) {
        return new UserResponse(
                profesor.getId(),
                profesor.getCorreo(),
                Role.PROFESOR,
                null,
                profesor.getColegio().getId(),
                profesor.getNombreCompleto());
    }
}
