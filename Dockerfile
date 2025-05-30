FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /application
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests
FROM eclipse-temurin:21-jre-jammy
WORKDIR /application
RUN groupadd -r appuser && useradd -r -g appuser appuser
COPY --from=builder /application/target/payroll-*.jar application.jar
RUN chown appuser:appuser application.jar
USER appuser
EXPOSE 8080
ENV JVM_OPTS="-Xmx512m -Xms256m -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar application.jar"]
