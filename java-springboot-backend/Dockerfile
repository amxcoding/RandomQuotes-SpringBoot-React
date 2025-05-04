# --- Build Stage ---
FROM eclipse-temurin:21-jdk-alpine AS backend-builder

ARG APIDIR=java-springboot-backend

# Set the working directory relative to the project root
WORKDIR /app

# Copy Maven wrapper and pom files first for dependency caching
COPY ${APIDIR}/randomquotes.api/mvnw .
COPY ${APIDIR}/randomquotes.api/.mvn .mvn

# Copy the PARENT pom.xml
COPY ${APIDIR}/pom.xml .
COPY ${APIDIR}/randomquotes.api/pom.xml randomquotes.api/
COPY ${APIDIR}/randomquotes.domain/pom.xml randomquotes.domain/
COPY ${APIDIR}/randomquotes.application/pom.xml randomquotes.application/
COPY ${APIDIR}/randomquotes.infrastructure/pom.xml randomquotes.infrastructure/
COPY ${APIDIR}/randomquotes.infrastructure.persistence/pom.xml randomquotes.infrastructure.persistence/

# Now copy the entire source code for all modules
COPY ${APIDIR}/randomquotes.api ./randomquotes.api
COPY ${APIDIR}/randomquotes.application ./randomquotes.application
COPY ${APIDIR}/randomquotes.domain ./randomquotes.domain
COPY ${APIDIR}/randomquotes.infrastructure ./randomquotes.infrastructure
COPY ${APIDIR}/randomquotes.infrastructure.persistence ./randomquotes.infrastructure.persistence

# Download dependencies based on the POM files
RUN ./mvnw dependency:go-offline

# Run tests and package. 'verify' runs tests first.
# If tests fail, this command fails, stopping the Docker build.
RUN ./mvnw verify

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine AS backend-runtime

WORKDIR /app

# Copy ONLY the final executable JAR from the correct module's target directory in the build stage.
COPY --from=backend-builder /app/randomquotes.api/target/*.jar app.jar

# Expose the internal port the Spring Boot app listens on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# --- Nginx Proxy ---
FROM nginx:alpine AS backend-nginx

ARG NDIR=nginx

# Remove the default Nginx configuration file
RUN rm /etc/nginx/conf.d/default.conf && rm /etc/nginx/nginx.conf

RUN apk add --no-cache supervisor

# Copy custom main Nginx configuration
COPY ${NDIR}/nginx.conf /etc/nginx/nginx.conf

# Copy site-specific configuration into the directory included by nginx.conf
COPY ${NDIR}/sites-enabled/randomquotes.api.conf /etc/nginx/conf.d/

# Expose port 80 (the port Nginx listens on inside the container)
EXPOSE 80