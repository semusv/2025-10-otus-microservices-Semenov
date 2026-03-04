FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY settings.gradle build.gradle ./
COPY auth-service/build.gradle ./auth-service/
COPY auth-service/src ./auth-service/src
RUN gradle :auth-service:build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

