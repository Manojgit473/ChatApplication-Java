# Multi-stage Dockerfile for Connect Chat
# Stage 1: Build the Maven application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
# Pre-fetch dependencies
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create upload directory and non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN mkdir -p /app/uploads/attachments && chown -R appuser:appgroup /app

USER appuser

COPY --from=builder /app/target/connect-chat-1.0.0.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
