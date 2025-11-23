# Stage 1: сборка приложения
FROM bellsoft/liberica-openjdk-alpine-musl:21.0.1 AS builder
WORKDIR /app
# Копируем только нужные файлы для сборки
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .
COPY simple-service/ simple-service/
RUN ./gradlew :simple-service:build -x test

# Stage 2: рантайм образ
FROM bellsoft/liberica-openjdk-alpine-musl:21.0.1
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
COPY --from=builder /app/simple-service/build/libs/*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]