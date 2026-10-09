package backend_r.reciclaje.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByIdentificadorIgnoreCase(String identificador);

    boolean existsByIdentificadorIgnoreCase(String identificador);

    boolean existsByRol(Role rol);
}
