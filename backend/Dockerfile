# Pulse Spring Boot Dockerfile

# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the JAR file, skipping tests to deploy faster
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the built JAR from the builder stage
COPY --from=build /app/target/pulse-backend-0.0.1-SNAPSHOT.jar app.jar
# Expose the default Spring Boot port
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
