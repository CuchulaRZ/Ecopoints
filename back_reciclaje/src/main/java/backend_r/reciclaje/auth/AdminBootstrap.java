package backend_r.reciclaje.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthService authService;
    private final UsuarioRepository usuarios;
    private final String identifier;
    private final String password;

    public AdminBootstrap(
            AuthService authService,
            UsuarioRepository usuarios,
            @Value("${app.bootstrap-admin.identifier:}") String identifier,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.authService = authService;
        this.usuarios = usuarios;
        this.identifier = identifier;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (identifier.isBlank() != password.isBlank()) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_IDENTIFIER y BOOTSTRAP_ADMIN_PASSWORD deben configurarse juntos");
        }
        if (identifier.isBlank()) {
            if (!usuarios.existsByRol(Role.ADMIN)) {
                logger.warn(
                        "No existe una cuenta ADMIN. El backend inicia, pero /api/admin/** queda sin usuarios autorizados; "
                                + "configura BOOTSTRAP_ADMIN_IDENTIFIER y BOOTSTRAP_ADMIN_PASSWORD para habilitar la administracion.");
            }
            return;
        }
        authService.createBootstrapAdmin(identifier, password);
    }
}
