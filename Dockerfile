# Multi-stage Dockerfile for the Micronaut Kotlin service

# Build stage: use the project root, no build inside Docker (we build locally)
FROM eclipse-temurin:17-jdk as build
WORKDIR /app
# Copy the fat jar produced by the shadow plugin
COPY build/libs/*.jar app.jar

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar .

# Expose port and entrypoint
EXPOSE 8080
ENV ENV_JAVA_OPTS="-Xms128m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $ENV_JAVA_OPTS -jar /app/app.jar"]
