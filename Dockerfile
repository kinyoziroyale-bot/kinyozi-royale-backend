# syntax=docker/dockerfile:1.6

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache dependencies
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Build
COPY src ./src
RUN mvn -B -DskipTests package \
 && cp target/*.jar /workspace/app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring

COPY --from=build /workspace/app.jar /app/app.jar

ENV JAVA_OPTS=""
EXPOSE 8081

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
