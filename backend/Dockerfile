FROM eclipse-temurin:17-jdk AS healthcheck
WORKDIR /tmp/health
COPY HealthCheck.java .
RUN javac -d /tmp/health HealthCheck.java

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY gateway/target/gateway-*.jar app.jar
COPY --from=healthcheck /tmp/health/HealthCheck.class HealthCheck.class
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
