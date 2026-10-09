package backend_r.reciclaje.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.database.initialize-support-schema", havingValue = "true", matchIfMissing = true)
@Profile("!test")
public class SupportSchemaInitializer implements ApplicationRunner {
    private static final Pattern CREATE_OBJECT = Pattern.compile(
            "(?i)^\\s*CREATE\\s+(TABLE|INDEX)\\s+([A-Z0-9_$#]+)\\b");

    private final JdbcTemplate jdbc;

    public SupportSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        requireBaseTable("ALUMNO");
        requireBaseTable("COLEGIO");
        requireBaseTable("PROFESOR");
        requireBaseTable("REGISTRO_PUNTOS");
        requireBaseColumn("REGISTRO_PUNTOS", "ID_PROFESOR");
        executeMissingObjects("db/oracle/01_auth_schema.sql");
        executeMissingObjects("db/oracle/02_dashboard_schema.sql");
    }

    private void requireBaseColumn(String tableName, String columnName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName.toUpperCase(),
                columnName.toUpperCase());
        if (count == null || count == 0) {
            throw new IllegalStateException(
                    "No existe la columna " + columnName + " en " + tableName
                            + ". Ejecuta la migracion del esquema base de Ecopoints.");
        }
    }

    private void requireBaseTable(String tableName) {
        if (!tableExists(tableName)) {
            throw new IllegalStateException(
                    "No existe la tabla base " + tableName
                            + " en el esquema conectado. Configura la conexión de Oracle al esquema de Ecopoints.");
        }
    }

    private void executeMissingObjects(String resourcePath) {
        String script = readScript(resourcePath)
                .replaceAll("(?m)^\\s*--.*(?:\\R|$)", "");

        for (String fragment : script.split(";")) {
            String statement = fragment.trim();
            if (statement.isEmpty()) {
                continue;
            }

            Matcher matcher = CREATE_OBJECT.matcher(statement);
            if (!matcher.find()) {
                throw new IllegalStateException(
                        "Sentencia no soportada en el script de esquema " + resourcePath);
            }

            String objectType = matcher.group(1);
            String objectName = matcher.group(2);
            boolean exists = "TABLE".equalsIgnoreCase(objectType)
                    ? tableExists(objectName)
                    : indexExists(objectName);
            if (!exists) {
                jdbc.execute(statement);
            }
        }
    }

    private String readScript(String resourcePath) {
        try (var input = new ClassPathResource(resourcePath).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el esquema " + resourcePath, exception);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = ?",
                Integer.class,
                tableName.toUpperCase());
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM USER_INDEXES WHERE INDEX_NAME = ?",
                Integer.class,
                indexName.toUpperCase());
        return count != null && count > 0;
    }
}
