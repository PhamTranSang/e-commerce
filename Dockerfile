# syntax=docker/dockerfile:1

# --- Build stage: compile and package the Spring Boot fat jar -----------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy build definition first so dependency resolution can be layer-cached.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Copy sources and build the runnable jar.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# --- Runtime stage: run the jar on a JRE as a non-root user -------------------
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Non-root runtime user.
RUN groupadd --system app && useradd --system --gid app --home /app app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
