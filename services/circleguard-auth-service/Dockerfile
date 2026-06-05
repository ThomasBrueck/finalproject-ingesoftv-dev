FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-auth-service/build.gradle.kts services/circleguard-auth-service/
RUN ./gradlew :services:circleguard-auth-service:dependencies --no-daemon || true
COPY services/circleguard-auth-service/src/ services/circleguard-auth-service/src/
RUN ./gradlew :services:circleguard-auth-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/services/circleguard-auth-service/build/libs/*.jar app.jar
EXPOSE 8180
ENTRYPOINT ["java", "-jar", "app.jar"]
