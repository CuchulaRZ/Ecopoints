package backend_r.reciclaje.auth;

import backend_r.reciclaje.domain.Profesor;
import backend_r.reciclaje.domain.ProfesorRepository;
import backend_r.reciclaje.domain.Alumno;
import backend_r.reciclaje.domain.AlumnoRepository;
import java.util.Optional;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    private final ProfesorRepository profesores;
    private final AlumnoRepository alumnos;

    public UsuarioDetailsService(
            UsuarioRepository usuarios,
            ProfesorRepository profesores,
            AlumnoRepository alumnos) {
        this.usuarios = usuarios;
        this.profesores = profesores;
        this.alumnos = alumnos;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        Optional<Profesor> profesor = findProfessor(identificador);
        if (profesor.isPresent()) {
            Profesor teacher = profesor.get();
            return User.withUsername(teacher.getId().toString())
                    .password(teacher.getPasswordHash())
                    .roles(Role.PROFESOR.name())
                    .disabled(!teacher.isActivo())
                    .build();
        }

        Optional<Usuario> usuario = usuarios.findByIdentificadorIgnoreCase(identificador);
        if (usuario.isPresent()) {
            Usuario account = usuario.get();
            return User.withUsername(account.getIdentificador())
                    .password(account.getPasswordHash())
                    .roles(account.getRol().name())
                    .disabled(!account.isActivo())
                    .build();
        }

        Optional<Alumno> alumno = findStudent(identificador);
        if (alumno.isPresent()) {
            return User.withUsername(alumno.get().getId().toString())
                    .password("")
                    .roles(Role.ALUMNO.name())
                    .build();
        }

        Alumno student = findStudent(identificador)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return User.withUsername(student.getId().toString())
                .password("")
                .roles(Role.ALUMNO.name())
                .build();
    }

    private Optional<Alumno> findStudent(String identificador) {
        try {
            return alumnos.findById(Long.valueOf(identificador));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private Optional<Profesor> findProfessor(String identificador) {
        try {
            return profesores.findById(Long.valueOf(identificador));
        } catch (NumberFormatException exception) {
            return profesores.findByCorreoIgnoreCase(identificador);
        }
    }
}
