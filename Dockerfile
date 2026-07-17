# syntax=docker/dockerfile:1.6

# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Cache Maven wrapper / dependencies
COPY mvnw* pom.xml ./
COPY .mvn .mvn
RUN if [ -f ./mvnw ]; then chmod +x ./mvnw && ./mvnw -B -q -DskipTests dependency:go-offline || true; fi

# Copy sources and build
COPY src src
RUN if [ -f ./mvnw ]; then \
      ./mvnw -B -DskipTests package; \
    else \
      apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/* && \
      mvn -B -DskipTests package; \
    fi \
 && cp target/*.jar app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Non-root user
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/app.jar /app/app.jar

# Render injects PORT; Spring reads it via server.port=${PORT:8081}
ENV JAVA_OPTS=""
EXPOSE 8081

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
