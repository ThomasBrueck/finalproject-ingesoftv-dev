FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-file-service/build.gradle.kts services/circleguard-file-service/
RUN ./gradlew :services:circleguard-file-service:dependencies --no-daemon || true
COPY services/circleguard-file-service/src/ services/circleguard-file-service/src/
RUN ./gradlew :services:circleguard-file-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/services/circleguard-file-service/build/libs/*.jar app.jar
EXPOSE 8280
ENTRYPOINT ["java", "-jar", "app.jar"]
