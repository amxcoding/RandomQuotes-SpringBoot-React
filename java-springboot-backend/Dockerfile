# --- Build Stage ---
FROM eclipse-temurin:21-jdk-alpine AS backend-builder

ARG APIDIR=java-springboot-backend

WORKDIR /app

COPY ${APIDIR}/randomquotes.api/mvnw .
COPY ${APIDIR}/randomquotes.api/.mvn .mvn

COPY ${APIDIR}/pom.xml .
COPY ${APIDIR}/randomquotes.api/pom.xml randomquotes.api/
COPY ${APIDIR}/randomquotes.domain/pom.xml randomquotes.domain/
COPY ${APIDIR}/randomquotes.application/pom.xml randomquotes.application/
COPY ${APIDIR}/randomquotes.infrastructure/pom.xml randomquotes.infrastructure/
COPY ${APIDIR}/randomquotes.infrastructure.persistence/pom.xml randomquotes.infrastructure.persistence/

COPY ${APIDIR}/randomquotes.api ./randomquotes.api
COPY ${APIDIR}/randomquotes.application ./randomquotes.application
COPY ${APIDIR}/randomquotes.domain ./randomquotes.domain
COPY ${APIDIR}/randomquotes.infrastructure ./randomquotes.infrastructure
COPY ${APIDIR}/randomquotes.infrastructure.persistence ./randomquotes.infrastructure.persistence

RUN ./mvnw dependency:go-offline

# Run tests and package. 'verify' runs tests first.
# If tests fail, this command fails, stopping the Docker build.
RUN ./mvnw verify

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine AS backend-runtime

WORKDIR /app

COPY --from=backend-builder /app/randomquotes.api/target/*.jar app.jar

# Expose the internal port the Spring Boot app listens on
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

# --- Nginx Proxy ---
FROM nginx:alpine AS backend-nginx

ARG NDIR=nginx

RUN rm /etc/nginx/conf.d/default.conf && rm /etc/nginx/nginx.conf

COPY ${NDIR}/nginx.conf /etc/nginx/nginx.conf

COPY ${NDIR}/sites-enabled/randomquotes.api.conf /etc/nginx/conf.d/

# Expose port 80 (the port Nginx listens on inside the container)
EXPOSE 80