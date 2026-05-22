# Stage 1: Build
# Use the official Maven image with Java 21 to compile and package the app.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first so Maven can download dependencies.
# Docker caches this layer — dependencies are only re-downloaded when pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source code and build the jar (skip tests for faster builds).
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Run
# Use a slim JRE image — no Maven or source code in the final image.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy only the built jar from the build stage.
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
