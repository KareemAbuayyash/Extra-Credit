# Build Stage - Maven with OpenJDK 21
FROM maven:3.9.8-eclipse-temurin-21 AS builder

# Application Build Directory
WORKDIR /application

# Maven Dependencies Caching Layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Source Code Copy and Build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage - Lightweight JRE
FROM eclipse-temurin:21-jre-jammy

# Application Runtime Directory
WORKDIR /application

# Security - Non-root User Creation
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Application JAR Copy
COPY --from=builder /application/target/payroll-*.jar application.jar

# File Ownership Configuration
RUN chown appuser:appuser application.jar

# Switch to Secure User Context
USER appuser

# Container Port Exposure
EXPOSE 8080

# JVM Optimization for Containers
ENV JVM_OPTS="-Xmx512m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Application Startup Command
ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar application.jar"]
