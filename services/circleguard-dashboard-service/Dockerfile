FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-dashboard-service/build.gradle.kts services/circleguard-dashboard-service/
RUN ./gradlew :services:circleguard-dashboard-service:dependencies --no-daemon || true
COPY services/circleguard-dashboard-service/src/ services/circleguard-dashboard-service/src/
RUN ./gradlew :services:circleguard-dashboard-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/services/circleguard-dashboard-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
