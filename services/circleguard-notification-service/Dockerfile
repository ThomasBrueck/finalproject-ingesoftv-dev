FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-notification-service/build.gradle.kts services/circleguard-notification-service/
RUN ./gradlew :services:circleguard-notification-service:dependencies --no-daemon || true
COPY services/circleguard-notification-service/src/ services/circleguard-notification-service/src/
RUN ./gradlew :services:circleguard-notification-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/services/circleguard-notification-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
