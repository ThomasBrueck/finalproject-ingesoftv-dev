FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-promotion-service/build.gradle.kts services/circleguard-promotion-service/
RUN ./gradlew :services:circleguard-promotion-service:dependencies --no-daemon || true
COPY services/circleguard-promotion-service/src/ services/circleguard-promotion-service/src/
RUN ./gradlew :services:circleguard-promotion-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/services/circleguard-promotion-service/build/libs/*.jar app.jar
EXPOSE 8580
ENTRYPOINT ["java", "-jar", "app.jar"]
