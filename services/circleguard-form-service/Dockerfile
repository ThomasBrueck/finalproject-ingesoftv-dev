FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-form-service/build.gradle.kts services/circleguard-form-service/
RUN ./gradlew :services:circleguard-form-service:dependencies --no-daemon || true
COPY services/circleguard-form-service/src/ services/circleguard-form-service/src/
RUN ./gradlew :services:circleguard-form-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/services/circleguard-form-service/build/libs/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
