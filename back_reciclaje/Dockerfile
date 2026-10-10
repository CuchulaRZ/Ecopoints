
# ETAPA 1: Compilar Spring Boot con Java 25
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copiar archivos del backend
COPY . .

# Dar permisos al Maven Wrapper
RUN chmod +x mvnw

# Compilar el proyecto
RUN ./mvnw -DskipTests clean package

# ETAPA 2: Ejecutar la aplicacion
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copiar el archivo JAR compilado
COPY --from=build /app/target/*.jar app.jar

# Puerto utilizado por Spring Boot
EXPOSE 8080

# Iniciar la aplicacion
ENTRYPOINT ["java", "-jar", "app.jar"]
