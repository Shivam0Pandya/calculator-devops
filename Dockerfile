# syntax=docker/dockerfile:1

# ---------- Stage 1: build the JAR with Maven + JDK ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy only the pom first so the dependency layer is cached
# and re-used until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy sources and build (tests already ran in the Jenkins Test stage).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: slim runtime with JRE only ----------
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="calculator" \
      org.opencontainers.image.description="Calculator REST API (Spring Boot)"

# Run as an unprivileged user
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build --chown=app:app /workspace/target/calculator.jar app.jar

USER app
EXPOSE 8080

# wget ships with alpine/busybox, so no extra packages are needed for the health probe
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
