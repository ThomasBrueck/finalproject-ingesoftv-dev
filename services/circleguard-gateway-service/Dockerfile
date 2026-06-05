FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-gateway-service/build.gradle.kts services/circleguard-gateway-service/
RUN ./gradlew :services:circleguard-gateway-service:dependencies --no-daemon || true
COPY services/circleguard-gateway-service/src/ services/circleguard-gateway-service/src/
RUN ./gradlew :services:circleguard-gateway-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/services/circleguard-gateway-service/build/libs/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
