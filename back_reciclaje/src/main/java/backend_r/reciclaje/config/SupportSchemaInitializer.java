
package backend_r.reciclaje.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
    name = "app.database.initialize-support-schema",
    havingValue = "true",
    matchIfMissing = false
)
@Profile("!test")
public class SupportSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public SupportSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {

        System.out.println(
            "Verificando estructura PostgreSQL de EcoPoints..."
        );

        // Comprobar las 8 tablas principales
        requireBaseTable("colegio");
        requireBaseTable("salon");
        requireBaseTable("alumno");
        requireBaseTable("profesor");
        requireBaseTable("usuario");
        requireBaseTable("actividad");
        requireBaseTable("registro_puntos");
        requireBaseTable("colegio_alumno");

        // Verificar columnas necesarias
        requireBaseColumn("alumno", "id_alumno");
        requireBaseColumn("alumno", "id_salon");

        requireBaseColumn("profesor", "id_profesor");
        requireBaseColumn("profesor", "id_colegio");
        requireBaseColumn("profesor", "id_salon");

        requireBaseColumn("registro_puntos", "id_registro");
        requireBaseColumn("registro_puntos", "id_alumno");
        requireBaseColumn("registro_puntos", "id_actividad");
        requireBaseColumn("registro_puntos", "id_profesor");
        requireBaseColumn("registro_puntos", "puntos");

        System.out.println(
            "Verificacion completada: tablas principales disponibles."
        );
    }

    private void requireBaseTable(String tableName) {

        if (!tableExists(tableName)) {
            throw new IllegalStateException(
                "No existe la tabla " + tableName
                + " en PostgreSQL. Verifica la base de datos Neon."
            );
        }
    }

    private void requireBaseColumn(
        String tableName,
        String columnName
    ) {

        String sql = """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = ?
              AND column_name = ?
            """;

        Integer count = jdbc.queryForObject(
            sql,
            Integer.class,
            tableName.toLowerCase(),
            columnName.toLowerCase()
        );

        if (count == null || count == 0) {
            throw new IllegalStateException(
                "No existe la columna "
                + columnName
                + " en la tabla "
                + tableName
            );
        }
    }

    private boolean tableExists(String tableName) {

        String sql = """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = current_schema()
              AND table_name = ?
              AND table_type = 'BASE TABLE'
            """;

        Integer count = jdbc.queryForObject(
            sql,
            Integer.class,
            tableName.toLowerCase()
        );

        return count != null && count > 0;
    }
}
