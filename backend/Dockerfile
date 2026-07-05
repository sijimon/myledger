# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache dependencies first
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
# Build the application
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- run stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Non-root user
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/target/backend-0.1.0.jar app.jar
# Receipts / attachments live outside any web root; mounted as a volume in compose.
RUN mkdir -p /data/files && chown -R app:app /data
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
